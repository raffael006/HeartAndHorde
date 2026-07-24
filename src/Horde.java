import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.List;

public class Horde implements Serializable {

    // Enum (Sistem Status & Tipe)
    // AXEMAN = 1Horde, SHIELDBEARER = 2Horde, BOWMAN = 3Horde
    public enum HordeType { AXEMAN, SHIELDBEARER, BOWMAN, BEAR, TWO_AXE, LOG, SORCERER }
    public enum HordeState { IDLE, MOVING, ATTACKING }

    // Atribut Identitas
    public HordeType type;
    public HordeState state;
    public double speed;

    // Atribut Posisi & Geometri
    public double x, y;
    public int size = 20; // Samakan dengan Guard agar proporsinya imbang

    // --- FITUR BARU: ARAH HADAP (Kiri/Kanan) ---
    // Default false = menghadap kiri (sesuai gambar asli 1Horde.png, 2Horde.png, 3Horde.png)
    public boolean facingRight = false;

    // Atribut Status Pertarungan (Samsak)
    public double maxHp;
    public double currentHp;
    public double attackDamage;

    // --- FITUR BARU: FLASH PUTIH pas kena hit ---
    public long lastHitTime = -999999;
    private static final long HIT_FLASH_DURATION = 110; // ms

    // Atribut Pertarungan Lanjutan
    public long lastAttackTime = 0;
    public long attackCooldown = 1200; // Musuh memukul sedikit lebih lambat (1.2 detik)
    public double attackRange = size + 5;
    // --- FITUR BARU: PATHFINDING BUAT JALAN NGEDEKATIN TARGET + FALLBACK HANCURIN TEMBOK ---
    public transient java.util.List<Point> path = null;
    public int pathIndex = 0;
    private long lastChasePathTime = 0;
    private static final long CHASE_REPATH_COOLDOWN = 600; // ms, jangan itung ulang A* tiap frame
    private Building forcedWallTarget = null; // Diisi kalau lagi kejebak & harus mecahin tembok dulu

    // --- FIX: BUG "GAK TAU MAU KEMANA" ---
    // Sebelumnya target (Guard/Civil/Building) dicari ulang dari nol TIAP FRAME murni berdasarkan
    // "siapa yang paling deket". Kalau ada 2 target yang jaraknya nyaris sama, hasil "paling deket"
    // bisa gonta-ganti tiap frame gara-gara horde-nya sendiri gerak dikit -> targetX/targetY ikut
    // lompat-lompat -> horde keliatan bingung/plin-plan gak jelas mau kemana.
    // Solusinya: kunci ("lock") ke satu target & baru pindah kalau target itu mati/hilang,
    // atau ada target lain yang JAUH LEBIH DEKET (bukan cuma dikit lebih deket).
    private Guard lockedGuard = null;
    private Civil lockedCivil = null;
    private Building lockedBuilding = null;
    private static final double TARGET_SWITCH_MARGIN = 40.0; // target baru harus lebih deket minimal segini buat ganti lock
    private int lastChosenTier = 0; // 0=none, 1=Guard, 2=Building, 3=Civil -- buat histeresis prioritas tier

    // --- FIX KEDUA: path jadi basi kalau target pindah ---
    // Sebelumnya re-path (hitung ulang A*) cuma dipicu oleh cooldown waktu, TIDAK peduli apakah
    // target yang dituju sudah berubah posisi/berganti. Akibatnya horde bisa tetap jalan menuju
    // titik target yang LAMA sampai cooldown-nya habis, baru "sadar" & belok mendadak ke target baru.
    private double lastMoveTargetX = Double.NaN;
    private double lastMoveTargetY = Double.NaN;
    private static final double TARGET_MOVE_REPATH_THRESHOLD = 30.0; // target geser lebih dari ini -> paksa repath

    // --- FIX KEEMPAT: bug "maju-mundur" di kerumunan padat (banyak horde numpuk nyerbu 1 target) ---
    // Sebelumnya status "di jarak serang" vs "harus maju" dicek pakai SATU angka jarak yang sama
    // tiap frame. Pas horde lagi rame numpuk deket target, dorongan anti-tumpuk dari tetangga bisa
    // ngegeser horde keluar dari radius serang dikit -> status jadi "harus maju" -> begitu maju
    // dikit, nyenggol tetangga lagi -> kedorong keluar lagi -> loop maju-mundur. Sekarang dikasih
    // "zona toleransi": begitu horde udah dianggap masuk jarak serang, dia baru dianggap "keluar
    // jangkauan lagi" kalau kedorong cukup jauh (bukan cuma nyerempet dikit).
    private boolean wasInAttackRange = false;
    private static final double ATTACK_RANGE_HYSTERESIS = 15.0;

    // --- FIX KEENAM: SISTEM ANTRIAN SLOT SERANG ---
    // Daripada semua horde yang niatin target yang sama maksa deket-deket (yang bikin
    // dorongan anti-tumpuk saling tarik-menarik terus di kerumunan padat), sekarang tiap
    // target (Guard/Building/Civil) punya jatah "slot" terbatas buat horde yang boleh
    // beneran nempel & nyerang. Horde yang gak kebagian slot cuma jalan sampe jarak aman
    // di luar kerumunan terus DIAM nunggu giliran -- gak ikut dorong-dorongan masuk.
    private Object reservedTarget = null;
    private boolean hasEngagementSlot = false;
    private static final int MAX_ENGAGERS_PER_TARGET = 6; // kira-kira muat segini di sekeliling 1 target
    private static final double QUEUE_GAP = 45.0; // jarak "antri" di luar jarak serang normal
    private static final java.util.Map<Object, java.util.Set<Horde>> engagementSlots = new java.util.HashMap<>();

    private void updateEngagementSlot(Object currentTargetObj) {
        if (currentTargetObj != reservedTarget) {
            releaseEngagementSlot();
            reservedTarget = currentTargetObj;
        }
        if (currentTargetObj == null) return;
        if (!hasEngagementSlot) {
            java.util.Set<Horde> slotSet = engagementSlots.computeIfAbsent(currentTargetObj, k -> new java.util.HashSet<>());
            if (slotSet.size() < MAX_ENGAGERS_PER_TARGET) {
                slotSet.add(this);
                hasEngagementSlot = true;
            }
        }
    }

    private void releaseEngagementSlot() {
        if (reservedTarget != null) {
            java.util.Set<Horde> set = engagementSlots.get(reservedTarget);
            if (set != null) set.remove(this);
        }
        reservedTarget = null;
        hasEngagementSlot = false;
    }

    public Horde(HordeType type, double startX, double startY) {
        this.type = type;

        // Geser anchor point agar pas di tengah kursor saat di-spawn
        this.x = startX - (size / 2.0);
        this.y = startY - (size / 2.0);
        this.state = HordeState.IDLE;

        applyStatsForType();

        // Darah penuh saat pertama kali spawn
        this.currentHp = this.maxHp;
    }

    // --- FITUR BARU: dipisah dari constructor supaya bisa dipanggil ulang pas Bear "ganti wujud" ---
    private void applyStatsForType() {
        // --- FITUR BARU: SEMUA maxHp dikali 3 (buff ketebalan darah horde) ---
        if (this.type == HordeType.AXEMAN) {
            this.maxHp = 80 * 3;
            this.attackDamage = 10;
            this.speed = 1;
            this.attackCooldown = 1200;
        } else if (this.type == HordeType.SHIELDBEARER) {
            this.maxHp = 150 * 3; // Darah paling tebal (Tank)
            this.attackDamage = 5;
            this.speed = 0.7; // Paling lambat
            this.attackCooldown = 1200;
        } else if (this.type == HordeType.BOWMAN) {
            this.maxHp = 50 * 3;  // Darah tipis
            this.attackDamage = 15;
            this.speed = 1;
            this.attackCooldown = 1200;
        } else if (this.type == HordeType.BEAR) {
            this.maxHp = 90 * 3;
            this.attackDamage = 10;
            this.speed = 1.15; // Dikit lebih cepet dari Bowman
            this.attackCooldown = 1200;
        } else if (this.type == HordeType.TWO_AXE) {
            this.maxHp = 80 * 3;
            this.attackDamage = 10;
            this.speed = 0.8;
            this.attackCooldown = 700; // Lebih barbar -> mukul lebih sering
        } else if (this.type == HordeType.LOG) {
            this.maxHp = 300 * 3; // Darah tebal banget
            this.attackDamage = 8;   // Ke Guard/Civil normal aja
            this.speed = 0.4;        // Lambat banget
            this.attackCooldown = 1800; // Ayunan berat, lama
        } else if (this.type == HordeType.SORCERER) {
            this.maxHp = 35 * 3; // Tipis, support unit
            this.attackDamage = 0; // Gak nyerang langsung
            this.speed = 0.6;
            this.attackCooldown = 9999;
        }
    }

    // --- FITUR BARU: dipanggil GamePanel pas ngecek horde mati. Bear khusus "ganti wujud", bukan mati beneran. ---
    public boolean isDead() {
        if (currentHp > 0) return false;
        if (type == HordeType.BEAR) {
            type = HordeType.AXEMAN;
            applyStatsForType();
            currentHp = maxHp;
            return false; // Belum mati, cuma ganti wujud
        }
        releaseEngagementSlot();
        return true;
    }

    // --- FITUR BARU: Multiplier damage kalau lagi di dalam aura Sorcerer terdekat ---
    private static final double SORCERER_AURA_RADIUS = 150.0;
    private static final double SORCERER_DAMAGE_MULTIPLIER = 1.5;
    private static final double SORCERER_HEAL_PER_TICK = 0.5;
    private static final double LOG_BUILDING_DAMAGE_MULTIPLIER = 5.0;
    private static final double FACING_DEADZONE = 8.0;

    private boolean isNearSorcerer(List<Horde> allHordes) {
        if (type == HordeType.SORCERER) return false; // Sorcerer gak nge-buff diri sendiri
        for (Horde h : allHordes) {
            if (h.type != HordeType.SORCERER || h == this) continue;
            double dx = h.x - x, dy = h.y - y;
            if (Math.sqrt(dx * dx + dy * dy) <= SORCERER_AURA_RADIUS) return true;
        }
        return false;
    }

    // Fungsi update menerima tambahan daftar Proyektil (GameWindow.activeProjectiles)
    // --- FITUR BARU: Tambahan parameter allCivils & allBuildings, biar Horde bisa niatin warga sipil & bangunan juga ---
    public void update(List<Horde> allHordes, List<Guard> allGuards, java.util.List<Projectile> allProjectiles, List<Civil> allCivils, List<Building> allBuildings) {

        // --- FITUR BARU: Sorcerer punya alur sendiri, gak ikut sistem targeting Guard/Building/Civil biasa ---
        if (type == HordeType.SORCERER) {
            updateSorcerer(allHordes, allBuildings);
            return;
        }

        long currentTime = System.currentTimeMillis();

        // --- FITUR BARU: Buff dari aura Sorcerer (ilang kalau keluar radius) ---
        boolean buffed = isNearSorcerer(allHordes);
        if (buffed) currentHp = Math.min(maxHp, currentHp + SORCERER_HEAL_PER_TICK);
        double effectiveDamage = attackDamage * (buffed ? SORCERER_DAMAGE_MULTIPLIER : 1.0);

        // Cari Guard terdekat untuk disamperin
        Guard targetGuard = null;
        double minDistance = 9999;
        for (Guard guard : allGuards) {
            double dx = this.x - guard.x;
            double dy = this.y - guard.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < minDistance) {
                minDistance = distance;
                targetGuard = guard;
            }
        }
        // --- FIX: histeresis, jangan gampang ganti-ganti Guard yang dikejar ---
        if (lockedGuard != null && allGuards.contains(lockedGuard) && lockedGuard.currentHp > 0
                && lockedGuard != targetGuard) {
            double dxL = this.x - lockedGuard.x, dyL = this.y - lockedGuard.y;
            double distToLocked = Math.sqrt(dxL * dxL + dyL * dyL);
            if (distToLocked <= minDistance + TARGET_SWITCH_MARGIN) {
                targetGuard = lockedGuard;
                minDistance = distToLocked;
            }
        }
        lockedGuard = targetGuard;

        // --- FITUR BARU: Cari Civil terdekat juga (yang gak lagi ngumpet aman di rumah) ---
        Civil targetCivil = null;
        double minCivilDistance = 9999;
        if (allCivils != null) {
            for (Civil c : allCivils) {
                if (c.hidden || c.currentHp <= 0) continue; // Civil yang lagi ngumpet gak bisa disamperin
                double dx = this.x - c.x;
                double dy = this.y - c.y;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < minCivilDistance) {
                    minCivilDistance = distance;
                    targetCivil = c;
                }
            }
        }
        // --- FIX: histeresis buat Civil juga ---
        if (lockedCivil != null && allCivils != null && allCivils.contains(lockedCivil)
                && !lockedCivil.hidden && lockedCivil.currentHp > 0 && lockedCivil != targetCivil) {
            double dxL = this.x - lockedCivil.x, dyL = this.y - lockedCivil.y;
            double distToLocked = Math.sqrt(dxL * dxL + dyL * dyL);
            if (distToLocked <= minCivilDistance + TARGET_SWITCH_MARGIN) {
                targetCivil = lockedCivil;
                minCivilDistance = distToLocked;
            }
        }
        lockedCivil = targetCivil;

        // --- FITUR BARU: Cari Building terdekat juga (yang udah jadi & masih ada darahnya) ---
        Building targetBuildingCandidate = null;
        double minBuildingDistance = 9999;
        if (allBuildings != null) {
            for (Building b : allBuildings) {
                if (!b.isBuilt || b.currentHp <= 0) continue;
                Rectangle hb = b.getSolidHitbox();
                double distance = distanceToRect(hb, this.x, this.y);
                if (distance < minBuildingDistance) {
                    minBuildingDistance = distance;
                    targetBuildingCandidate = b;
                }
            }
        }
        // --- FIX: histeresis buat Building juga ---
        if (lockedBuilding != null && allBuildings != null && allBuildings.contains(lockedBuilding)
                && lockedBuilding.isBuilt && lockedBuilding.currentHp > 0 && lockedBuilding != targetBuildingCandidate) {
            double distToLocked = distanceToRect(lockedBuilding.getSolidHitbox(), this.x, this.y);
            if (distToLocked <= minBuildingDistance + TARGET_SWITCH_MARGIN) {
                targetBuildingCandidate = lockedBuilding;
                minBuildingDistance = distToLocked;
            }
        }
        lockedBuilding = targetBuildingCandidate;

        // --- FITUR BARU: PRIORITAS BERTINGKAT (bukan lagi 'yang paling deket menang') ---
        // 1. Guard yang ada DI SEKITAR horde ini -> prioritas utama
        // 2. Kalau gak ada Guard di sekitar -> serang Building terdekat
        // 3. Kalau Building di sekitar udah abis (gak ada yang deket) & ada Civil -> kejar Civil
        // 4. Tapi kalau ngejar Civil udah kejauhan dari Building manapun -> nyerah, balik hancurin Building lain (termasuk Heart)
        final double AGGRO_GUARD_RADIUS = 250.0;     // Guard dianggap "di sekitar" kalau sedeket ini
        final double AGGRO_BUILDING_RADIUS = 250.0;  // Building dianggap "di sekitar" kalau sedeket ini
        final double CIVIL_CHASE_LEASH = 350.0;      // Batas jauh ngejar Civil sebelum nyerah balik ke Building

        // --- FIX KEDELAPAN: histeresis level TIER (Guard vs Building vs Civil), bukan cuma
        // level individu target. Sebelumnya hasNearGuard/hasNearBuilding dicek pakai radius
        // tetap 250 tiap frame -> kalau horde-nya posisinya pas nyerempet radius itu (baik
        // gara-gara dia sendiri gerak dikit, atau Guard/Building targetnya yg gerak), status
        // ini bisa nyala-mati tiap frame -> targetX/targetY lompat total antar kategori yang
        // beda jauh posisinya -> horde keliatan maju-mundur/plin-plan. Cuma horde yang posisinya
        // KEBETULAN di sekitar batas 250 ini yang kena, sisanya (jelas dalem/luar radius) aman --
        // makanya cuma "beberapa doang" yang keliatan bug. Sekarang tier yang lagi aktif dikasih
        // "bonus" radius biar gak gampang lepas cuma gara-gara nyerempet dikit.
        double guardRadiusEff = AGGRO_GUARD_RADIUS + (lastChosenTier == 1 ? TARGET_SWITCH_MARGIN : 0);
        double buildingRadiusEff = AGGRO_BUILDING_RADIUS + (lastChosenTier == 2 ? TARGET_SWITCH_MARGIN : 0);

        boolean hasNearGuard = targetGuard != null && minDistance <= guardRadiusEff;
        boolean hasNearBuilding = targetBuildingCandidate != null && minBuildingDistance <= buildingRadiusEff;

        boolean attackingCivil = false;
        boolean attackingBuilding = false;
        Building targetBuilding = null;
        double targetX = 0, targetY = 0, minTargetDistance = -1;

        if (hasNearGuard) {
            // 1. Guard di sekitar -> prioritas utama
            targetX = targetGuard.x;
            targetY = targetGuard.y;
            minTargetDistance = minDistance;
        } else if (hasNearBuilding) {
            // 2. Gak ada Guard di sekitar -> serang Building terdekat
            attackingBuilding = true;
            targetBuilding = targetBuildingCandidate;
            Point2D.Double nearestPoint = nearestPointOnRect(targetBuildingCandidate.getSolidHitbox(), this.x, this.y);
            targetX = nearestPoint.x;
            targetY = nearestPoint.y;
            minTargetDistance = minBuildingDistance;
        } else if (targetCivil != null) {
            // 3. Building di sekitar udah abis, ada Civil -> kejar, KECUALI udah kejauhan dari Building manapun (leash)
            boolean tooFarFromAnyBuilding = targetBuildingCandidate != null && minBuildingDistance > CIVIL_CHASE_LEASH;
            if (tooFarFromAnyBuilding) {
                // 4. Nyerah ngejar Civil -> balik hancurin Building lain (walau jauh, termasuk Heart)
                attackingBuilding = true;
                targetBuilding = targetBuildingCandidate;
                Point2D.Double nearestPoint = nearestPointOnRect(targetBuildingCandidate.getSolidHitbox(), this.x, this.y);
                targetX = nearestPoint.x;
                targetY = nearestPoint.y;
                minTargetDistance = minBuildingDistance;
            } else {
                attackingCivil = true;
                targetX = targetCivil.x;
                targetY = targetCivil.y;
                minTargetDistance = minCivilDistance;
            }
        } else if (targetBuildingCandidate != null) {
            // Fallback: gak ada Guard/Civil sama sekali, tapi masih ada Building -> tetep samperin walau jauh
            attackingBuilding = true;
            targetBuilding = targetBuildingCandidate;
            Point2D.Double nearestPoint = nearestPointOnRect(targetBuildingCandidate.getSolidHitbox(), this.x, this.y);
            targetX = nearestPoint.x;
            targetY = nearestPoint.y;
            minTargetDistance = minBuildingDistance;
        } else if (targetGuard != null) {
            // Fallback terakhir: Guard jauh tapi gak ada target lain sama sekali
            targetX = targetGuard.x;
            targetY = targetGuard.y;
            minTargetDistance = minDistance;
        }

        // --- Kalau lagi kejebak & harus mecahin tembok, override target ke tembok itu ---
        if (forcedWallTarget != null) {
            if (forcedWallTarget.currentHp <= 0 || !forcedWallTarget.isBuilt) {
                forcedWallTarget = null; // Tembok udah hancur, lepas override
            } else {
                attackingBuilding = true;
                attackingCivil = false;
                targetBuilding = forcedWallTarget;
                Point2D.Double np = nearestPointOnRect(forcedWallTarget.getSolidHitbox(), this.x, this.y);
                targetX = np.x;
                targetY = np.y;
                minTargetDistance = distanceToRect(forcedWallTarget.getSolidHitbox(), this.x, this.y);
            }
        }

        // Simpen tier yang akhirnya kepilih frame ini, buat dipake sebagai bonus histeresis
        // di frame BERIKUTNYA (lihat guardRadiusEff/buildingRadiusEff di atas).
        if (attackingCivil) lastChosenTier = 3;
        else if (attackingBuilding) lastChosenTier = 2;
        else if (targetGuard != null) lastChosenTier = 1;
        else lastChosenTier = 0;



        // --- Tentuin objek target final (buat sistem antrian slot) & update reservasinya ---
        Object currentTargetObj = attackingCivil ? targetCivil
                : (attackingBuilding ? targetBuilding : targetGuard);
        updateEngagementSlot(currentTargetObj);

        if (minTargetDistance >= 0) {
            double baseRange = (type == HordeType.BOWMAN) ? 250.0 : (size + 5);

            if (!hasEngagementSlot) {
                // FIX: gak kebagian slot -> jangan ikut nyerbu masuk ke kerumunan.
                // Jalan sampe jarak "antri" aman di luar radius serang, terus diem nunggu giliran.
                wasInAttackRange = false;
                double waitDistance = baseRange + QUEUE_GAP;
                if (minTargetDistance > waitDistance) {
                    moveTowardTarget(targetX, targetY, allBuildings);
                }
                // kalau udah di dalam waitDistance -> diem aja, gak maju lagi & gak nyerang
            } else if (type == HordeType.BOWMAN) {
                // --- LOGIKA BOWMAN (PANAH) ---
                double attackRangePanahMusuh = baseRange; // Sedikit lebih pendek dari player biar imbang
                double effectiveRangeBowman = wasInAttackRange
                        ? attackRangePanahMusuh + ATTACK_RANGE_HYSTERESIS : attackRangePanahMusuh;

                if (minTargetDistance <= effectiveRangeBowman) {
                    wasInAttackRange = true;
                    // Masuk jarak tembak: Berhenti dan Tembak (Cek cooldown)
                    // --- FITUR BARU: Saat menembak, tetap hadap ke arah target ---
                    if (targetX > this.x + FACING_DEADZONE) facingRight = true;
                    else if (targetX < this.x - FACING_DEADZONE) facingRight = false;

                    if (currentTime - lastAttackTime >= attackCooldown) {
                        if (attackingBuilding) {
                            // --- FITUR BARU: Panah ke Building damage langsung ---
                            // (sistem collision Projectile yang ada cuma ngecek nabrak Horde/Guard, belum Building,
                            // jadi biar Bowman tetap bisa "menembak" bangunan, damage-nya langsung dikenakan)
                            allProjectiles.add(new Projectile(x, y, targetX, targetY, false, 0));
                            targetBuilding.currentHp -= effectiveDamage;
                            targetBuilding.lastHitTime = currentTime;
                            HitParticles.burst(targetBuilding.getBounds().getCenterX(), targetBuilding.getBounds().getCenterY(), new Color(160, 140, 90), 5);
                        } else {
                            // BUAT PROYEKTIL BARU (false = dari horde, damage 15)
                            allProjectiles.add(new Projectile(x, y, targetX, targetY, false, effectiveDamage));
                        }
                        lastAttackTime = currentTime;
                    }
                } else {
                    wasInAttackRange = false;
                    // Di luar jarak tembak: Maju! (sekarang pakai PathFinder, bukan garis lurus)
                    moveTowardTarget(targetX, targetY, allBuildings);
                }

            } else {
                // --- LOGIKA MELEE (AXEMAN & SHIELD - Tetap aslinya) ---
                double attackRangeMeleeMusuh = baseRange;
                double effectiveRangeMelee = wasInAttackRange
                        ? attackRangeMeleeMusuh + ATTACK_RANGE_HYSTERESIS : attackRangeMeleeMusuh;
                if (minTargetDistance <= effectiveRangeMelee) {
                    wasInAttackRange = true;
                    // --- FITUR BARU: Saat memukul, tetap hadap ke arah target ---
                    if (targetX > this.x + FACING_DEADZONE) facingRight = true;
                    else if (targetX < this.x - FACING_DEADZONE) facingRight = false;

                    if (currentTime - lastAttackTime >= attackCooldown) {
                        if (attackingCivil) {
                            targetCivil.currentHp -= effectiveDamage;
                            HitParticles.burst(targetCivil.x + targetCivil.size / 2.0, targetCivil.y + targetCivil.size / 2.0, new Color(200, 30, 30), 6);
                        } else if (attackingBuilding) {
                            double dmg = effectiveDamage * (type == HordeType.LOG ? LOG_BUILDING_DAMAGE_MULTIPLIER : 1.0);
                            targetBuilding.currentHp -= dmg;
                            targetBuilding.lastHitTime = currentTime;
                            HitParticles.burst(targetBuilding.getBounds().getCenterX(), targetBuilding.getBounds().getCenterY(), new Color(160, 140, 90), 5);
                        } else {
                            targetGuard.currentHp -= effectiveDamage;
                            targetGuard.lastHitTime = currentTime;
                            HitParticles.burst(targetGuard.x + targetGuard.size / 2.0, targetGuard.y + targetGuard.size / 2.0, new Color(200, 30, 30), 6);
                        }
                        lastAttackTime = currentTime;
                    }
                } else {
                    wasInAttackRange = false;
                    // Di luar jarak serang: Maju! (sekarang pakai PathFinder, bukan garis lurus)
                    moveTowardTarget(targetX, targetY, allBuildings);
                }
            }
        }

        // --- Logika Anti-Tumpuk (Dipertahankan, tapi sekarang di-cap) ---
        // FIX: Sebelumnya dorongan dari SETIAP tetangga (Horde lain + Guard) langsung
        // ditambahin ke this.x/this.y tanpa batas. Kalau lagi rame (banyak horde numpuk
        // nyerbu bareng), total dorongan dalam 1 frame bisa lebih gede dari `speed` horde
        // itu sendiri -> gerakan "menuju target" ke-overwrite sama dorongan yang arahnya
        // acak & berubah-ubah tiap frame (tergantung siapa aja yang deket saat itu).
        // Itu yang bikin horde keliatan zigzag/plin-plan random pas lagi rame-rame.
        // Solusi: akumulasi dulu semua dorongan, baru di-cap magnitude-nya sebelum diterapkan.
        int personalSpace = size - 5;
        double pushX = 0, pushY = 0;
        for (Horde other : allHordes) {
            if (other == this) continue;
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq == 0) {
                pushX += Math.random() * 2 - 1; pushY += Math.random() * 2 - 1; continue;
            }
            if (distanceSq < personalSpace * personalSpace) {
                double distance = Math.sqrt(distanceSq);
                double pushForce = (personalSpace - distance) / personalSpace;
                pushX += (dx / distance) * pushForce * 1.5;
                pushY += (dy / distance) * pushForce * 1.5;
            }
        }
        for (Guard guard : allGuards) {
            // FIX KELIMA: kalau horde lagi aktif nyerang (nempel di jarak serang), dia MEMANG
            // seharusnya deket sama Guard di sekitarnya -- itu bukan "numpuk" yang perlu
            // dirapiin. Sebelumnya push ini tetap jalan walau lagi nyerang, jadi ada tarik-menarik
            // terus-terusan antara "narik deket buat mukul" vs "didorong anti-tumpuk" tiap frame,
            // apalagi kalau Guard-nya emang sengaja dibikin formasi padat (statis, gak minggir).
            // Sekarang: kalau lagi nempel nyerang, gak usah didorong-dorong lagi oleh Guard.
            if (wasInAttackRange) continue;

            double dx = this.x - guard.x;
            double dy = this.y - guard.y;
            double distanceSq = dx * dx + dy * dy;
            if (distanceSq < personalSpace * personalSpace && distanceSq > 0) {
                double distance = Math.sqrt(distanceSq);
                double pushForce = (personalSpace - distance) / personalSpace;
                pushX += (dx / distance) * pushForce * 1.5;
                pushY += (dy / distance) * pushForce * 1.5;
            }
        }

        double pushMag = Math.sqrt(pushX * pushX + pushY * pushY);
        double maxPush = Math.max(speed * 1.2, 0.8); // gak boleh lebih kuat dari gerak normal horde
        if (pushMag > maxPush) {
            pushX = (pushX / pushMag) * maxPush;
            pushY = (pushY / pushMag) * maxPush;
        }
        this.x += pushX;
        this.y += pushY;
    }


    // --- FITUR BARU: Sorcerer cuma ngikutin rombongan horde terdekat, gak nyerang langsung ---
    private void updateSorcerer(List<Horde> allHordes, List<Building> allBuildings) {
        Horde nearestAlly = null;
        double bestDist = Double.MAX_VALUE;
        for (Horde h : allHordes) {
            if (h == this || h.type == HordeType.SORCERER) continue;
            double dx = h.x - x, dy = h.y - y;
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d < bestDist) { bestDist = d; nearestAlly = h; }
        }

        if (nearestAlly != null && bestDist > SORCERER_AURA_RADIUS * 0.6) {
            // Terlalu jauh dari rombongan -> samperin dikit biar tetep dalem jangkauan buff
            moveTowardTarget(nearestAlly.x, nearestAlly.y, allBuildings);
        }
        // Kalau udah cukup deket rombongan, diem aja di situ (gak perlu ngapa-ngapain lagi)
    }

    // --- FITUR BARU: Helper hitung jarak dari titik (Horde) ke sisi terdekat rectangle Building ---
    // Dipakai supaya jangkauan serang/deteksi Building tetap akurat walau bangunannya gede,
    // (bukan ngukur ke titik tengah bangunan, yang bisa nyangkut jauh di dalam hitbox solid).
    private double distanceToRect(Rectangle r, double px, double py) {
        double cx = Math.max(r.x, Math.min(px, r.x + r.width));
        double cy = Math.max(r.y, Math.min(py, r.y + r.height));
        double dx = px - cx, dy = py - cy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private Point2D.Double nearestPointOnRect(Rectangle r, double px, double py) {
        double cx = Math.max(r.x, Math.min(px, r.x + r.width));
        double cy = Math.max(r.y, Math.min(py, r.y + r.height));
        return new Point2D.Double(cx, cy);
    }
    // Kalau target gak reachable sama sekali (terkurung tembok tanpa celah),
    // otomatis alihkan buat ngehancurin Wall terdekat yang beneran reachable dulu.
    // --- FIX KETIGA: bug "maju-mundur" ---
    // Sebelumnya hasClearLine() dicek di paling awal moveTowardTarget() TIAP FRAME, lepas dari
    // throttle repath. Efeknya horde bisa gonta-ganti mode "jalan lurus" vs "ikutin path A*"
    // tiap frame: pas kebetulan hasClearLine() bilang "clear" -> jalan lurus (kadang malah
    // nabrak nyerempet tembok), lalu frame berikutnya jadi "gak clear" -> switch ke path A*
    // yang rute-nya balik ngejauhin tembok dulu -> begitu mundur dikit "clear" lagi -> maju
    // lurus lagi -> looping maju-mundur. Sekarang keputusan mode cuma diambil ulang di siklus
    // repath yang sama (throttled), bukan tiap frame.
    private boolean usingDirectLine = false;

    private void moveTowardTarget(double tx, double ty, List<Building> allBuildings) {
        long now = System.currentTimeMillis();
        boolean targetMoved = Double.isNaN(lastMoveTargetX)
                || Math.hypot(tx - lastMoveTargetX, ty - lastMoveTargetY) > TARGET_MOVE_REPATH_THRESHOLD;
        boolean needRepath = path == null || (path.isEmpty() && !usingDirectLine)
                || pathIndex >= path.size() || now - lastChasePathTime > CHASE_REPATH_COOLDOWN || targetMoved;

        if (needRepath) {
            lastChasePathTime = now;
            lastMoveTargetX = tx;
            lastMoveTargetY = ty;

            if (hasClearLine(x, y, tx, ty, allBuildings)) {
                usingDirectLine = true;
                path = null;
            } else {
                usingDirectLine = false;
                List<Point> newPath = PathFinder.findPath(x, y, tx, ty, allBuildings);

                if (newPath.isEmpty() && allBuildings != null) {
                    Building nearestWall = findNearestReachableWall(allBuildings);
                    if (nearestWall != null) {
                        forcedWallTarget = nearestWall;
                        Point2D.Double np = nearestPointOnRect(nearestWall.getSolidHitbox(), x, y);
                        newPath = PathFinder.findPath(x, y, np.x, np.y, allBuildings);
                    } else {
                        forcedWallTarget = null; // Beneran gak ada jalan sama sekali, jangan nyangkut ke wall lama
                    }
                } else {
                    forcedWallTarget = null;
                }

                path = newPath;
                pathIndex = 0;
            }
        }

        if (usingDirectLine) {
            double dx = tx - x, dy = ty - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 1) {
                if (dx > FACING_DEADZONE) facingRight = true;
                else if (dx < -FACING_DEADZONE) facingRight = false;
                x += (dx / dist) * speed;
                y += (dy / dist) * speed;
            }
            return;
        }

        if (path != null && pathIndex < path.size()) {
            Point node = path.get(pathIndex);
            double nx = node.x - size / 2.0;
            double ny = node.y - size / 2.0;
            double dx = nx - x, dy = ny - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            // Arah gerak tetap ikut node path (biar bisa belok ngindarin tembok),
            // tapi arah HADAP dihitung dari target asli biar gak kedip2 tiap path nangga.
            double facingDx = tx - x;

            if (dist > 12.0) {
                if (facingDx > FACING_DEADZONE) facingRight = true;
                else if (facingDx < -FACING_DEADZONE) facingRight = false;
                x += (dx / dist) * speed;
                y += (dy / dist) * speed;
            } else {
                pathIndex++;
            }
        }
    }
    private boolean hasClearLine(double x1, double y1, double x2, double y2, java.util.List<Building> buildings) {
        if (buildings == null) return true;
        double dx = x2 - x1, dy = y2 - y1;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) return true;

        int steps = (int) (dist / 15) + 1; // Cek tiap ~15 unit sepanjang garis
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double px = x1 + dx * t;
            double py = y1 + dy * t;
            for (Building b : buildings) {
                if (b.getSolidHitbox().contains(px, py)) return false;
            }
        }
        return true;
    }

    // --- FITUR BARU: Cari Wall terdekat yang beneran bisa dijangkau (path-nya gak kosong) ---
    // --- FIX KETUJUH: bug "sebagian horde doang" maju-mundur -> ini pemilihan tembok fallback
    // yang gak punya histeresis sama sekali, beda dari target Guard/Building/Civil yang udah
    // di-lock. Tiap kali repath (~600ms), fungsi ini nyari ULANG dari nol tembok mana yang
    // paling deket. Horde ini sendiri gerak dikit tiap frame, jadi kalau ada 2+ tembok yang
    // jaraknya nyaris sama, hasil "paling deket" bisa keganti-ganti tiap siklus repath ->
    // forcedWallTarget lompat dari tembok A ke B ke A lagi -> horde balik arah tiap kali itu
    // kejadian. HANYA horde yang emang kebentur skenario "harus jebol tembok" ini yang kena,
    // makanya cuma sebagian horde doang yang keliatan maju-mundur, sisanya normal.
    private Building findNearestReachableWall(List<Building> allBuildings) {
        Building nearest = null;
        double bestDist = Double.MAX_VALUE;

        // Kalau lagi udah megang satu wall target & itu masih valid+reachable, kasih "diskon"
        // jarak (TARGET_SWITCH_MARGIN) biar gak gampang ke-switch ke wall lain yang cuma
        // dikit lebih deket.
        if (forcedWallTarget != null && forcedWallTarget.isBuilt && forcedWallTarget.currentHp > 0) {
            double d = distanceToRect(forcedWallTarget.getSolidHitbox(), x, y);
            List<Point> p = PathFinder.findPath(x, y,
                    forcedWallTarget.getSolidHitbox().getCenterX(), forcedWallTarget.getSolidHitbox().getCenterY(), allBuildings);
            if (!p.isEmpty()) {
                bestDist = Math.max(0, d - TARGET_SWITCH_MARGIN);
                nearest = forcedWallTarget;
            }
        }

        for (Building b : allBuildings) {
            if (!b.isBuilt || b.currentHp <= 0) continue;
            if (b.type != Building.BuildingType.WALL_L
                    && b.type != Building.BuildingType.WALL_R
                    && b.type != Building.BuildingType.WALL_UD) continue;
            if (b == forcedWallTarget) continue; // udah dicek duluan di atas

            double d = distanceToRect(b.getSolidHitbox(), x, y);
            if (d >= bestDist) continue;

            List<Point> p = PathFinder.findPath(x, y,
                    b.getSolidHitbox().getCenterX(), b.getSolidHitbox().getCenterY(), allBuildings);
            if (!p.isEmpty()) {
                bestDist = d;
                nearest = b;
            }
        }
        return nearest;
    }

    // --- FITUR BARU: Ukuran VISUAL gambar per tipe (beda dari hitbox `size` yang tetap kotak) ---
    public static int getVisualWidth(HordeType type) {
        if (type == HordeType.BEAR) return 38;   // Sesuaikan sama rasio Bear_Horde.png asli
        if (type == HordeType.LOG) return 58;    // Sesuaikan sama rasio Log_horde.png asli
        if (type == HordeType.SORCERER) return 20;
        if (type == HordeType.TWO_AXE) return 20;
        return 20; // Default (Axeman/Shieldbearer/Bowman) tetap 20x20 kayak sebelumnya
    }

    public static int getVisualHeight(HordeType type) {
        if (type == HordeType.BEAR) return 34;
        if (type == HordeType.LOG) return 25;    // Log kelihatannya "banner" lebar-pendek dari gambarnya
        if (type == HordeType.SORCERER) return 35;
        if (type == HordeType.TWO_AXE) return 20;
        return 20;
    }

    public void draw(Graphics2D g2d, BufferedImage img) {

        // --- FITUR BARU: Lingkaran radius buff Sorcerer (dipipihin biar sesuai perspektif 2.5D) ---
        if (type == HordeType.SORCERER) {
            int auraW = (int) (SORCERER_AURA_RADIUS * 2);
            int auraH = (int) (SORCERER_AURA_RADIUS * 2 * 0.5); // Rasio 1:2, sama kayak oval bayangan unit lain

            int cx = (int) (x + size / 2.0);
            int cy = (int) (y + size); // Nempel ke kaki/dasar sprite, bukan tengah body

            int auraX = cx - auraW / 2;
            int auraY = cy - auraH / 2;

            g2d.setColor(new Color(200, 0, 0, 40));
            g2d.fillOval(auraX, auraY, auraW, auraH);
            g2d.setColor(new Color(200, 0, 0, 120));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(auraX, auraY, auraW, auraH);
        }

        // --- LAYER 1: AURA MERAH (Pengganti lingkaran seleksi) ---
        g2d.setColor(new Color(200, 0, 0, 100)); // Merah transparan
        int ovalWidth = (int) (size * 0.50);
        int ovalHeight = (int) ((size / 2) * 0.50);
        int ovalX = (int)x + (size - ovalWidth) / 2 +3;
        int ovalY = (int)y + size - (ovalHeight / 2) - 4; // Posisinya sama persis dengan Guard
        g2d.fillOval(ovalX, ovalY, ovalWidth, ovalHeight);

        // --- LAYER 2: GAMBAR HORDE ---
        if (img != null) {
            int vw = getVisualWidth(type);
            int vh = getVisualHeight(type);
            // Gambar dipusatkan (center) di atas titik hitbox, biar gambar yang lebih
            // besar/kecil dari hitbox tetap "nempel" pas di tengah, bukan geser ke pojok.
            int drawX = (int) x + (size - vw) / 2;
            int drawY = (int) y + (size - vh) / 2;

            // --- FITUR BARU: Flip gambar horizontal kalau lagi menghadap kanan ---
            if (facingRight) {
                java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
                g2d.translate(drawX + vw, drawY);
                g2d.scale(-1, 1);
                g2d.drawImage(img, 0, 0, vw, vh, null);
                g2d.setTransform(oldTransform);
            } else {
                g2d.drawImage(img, drawX, drawY, vw, vh, null);
            }

            // --- FITUR BARU: FLASH PUTIH pas abis kena hit (fade out cepat) ---
            long sinceHit = System.currentTimeMillis() - lastHitTime;
            if (sinceHit >= 0 && sinceHit < HIT_FLASH_DURATION) {
                float t = 1f - (float) sinceHit / HIT_FLASH_DURATION;
                Composite oldComposite = g2d.getComposite();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, Math.min(1f, Math.max(0f, t))));
                g2d.setColor(Color.WHITE);
                g2d.fillRect(drawX, drawY, vw, vh);
                g2d.setComposite(oldComposite);
            }
        } else {
            g2d.setColor(Color.RED);
            g2d.fillRect((int)x, (int)y, size, size);
        }

        // --- LAYER 3: BAR DARAH MUSUH ---
        int barWidth = size;
        int barHeight = 4;
        int barX = (int) x;
        int barY = (int) y - 8; // Posisi bar darah disamakan dengan Guard

        // Background merah gelap
        g2d.setColor(new Color(150, 0, 0));
        g2d.fillRect(barX, barY, barWidth, barHeight);

        // Darah merah terang
        double hpPercentage = currentHp / maxHp;
        if (hpPercentage < 0) hpPercentage = 0;
        int currentBarWidth = (int) (barWidth * hpPercentage);

        g2d.setColor(new Color(255, 50, 50));
        g2d.fillRect(barX, barY, currentBarWidth, barHeight);

        // Outline hitam
        g2d.setColor(Color.BLACK);
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }
}