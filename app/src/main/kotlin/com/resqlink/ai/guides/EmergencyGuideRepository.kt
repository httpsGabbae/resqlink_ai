package com.resqlink.ai.guides

/**
 * # EmergencyGuideRepository
 *
 * 100% Offline Emergency Guide Repository containing bilingual (English & Filipino)
 * life-saving protocols verified against authoritative Philippine disaster & health agencies:
 * - Philippine Red Cross (PRC Hotline: 143 / 02-8527-0000)
 * - PHIVOLCS (Earthquake & Tsunami guidelines)
 * - Bureau of Fire Protection (BFP Hotline: 02-8426-0219)
 * - PAGASA (Typhoon, Storm Surge, Flood advisories)
 * - Department of Health (DOH)
 *
 * Ordinary Human Overview:
 * When cellular towers lose power during a typhoon or earthquake, you cannot Google first aid.
 * This repository keeps complete, authoritative first-aid and disaster protocols directly
 * in the app's offline binary, with fast instant-search in both English and Filipino.
 */
object EmergencyGuideRepository {

    private val cachedEmergencyGuidesList: List<EmergencyGuide> by lazy {
        listOf(
            // ==========================================
            // 1. CPR (ENGLISH)
            // ==========================================
            EmergencyGuide(
                id = "cpr-en",
                category = GuideCategory.CPR,
                language = GuideLanguage.ENGLISH,
                title = "Hands-Only CPR (Cardiac Arrest)",
                summary = "Immediate chest compressions maintain vital blood flow to the brain and heart when a person is unresponsive and not breathing normally.",
                steps = listOf(
                    GuideStep(1, "Check Responsiveness & Breathing", "Tap the victim's shoulders firmly and shout: 'Are you OK?'. Look at the chest for 5-10 seconds to check for normal breathing. Gasping is not normal breathing.", "If unresponsive and not breathing normally, shout for help and have someone call 911 or Red Cross 143 immediately."),
                    GuideStep(2, "Position Your Hands", "Place the heel of one hand in the center of the chest (lower half of breastbone). Interlock your other hand on top. Keep your elbows locked straight.", null),
                    GuideStep(3, "Compress Hard and Fast", "Push hard and fast in the center of the chest at 100 to 120 beats per minute (to the beat of 'Stayin' Alive'). Compress at least 2 inches (5 cm) deep.", "Allow complete chest recoil between compressions without lifting hands off."),
                    GuideStep(4, "Continue Until Relief Arrives", "Do not stop compressions until trained medical personnel arrive, an AED is ready to use, or the person clearly recovers consciousness.", null)
                ),
                whatNotToDo = listOf(
                    "DO NOT delay compressions to check for a pulse if you are an untrained lay responder.",
                    "DO NOT lean on the victim's chest between compressions; allow the chest to fully rise.",
                    "DO NOT interrupt compressions for more than 10 seconds."
                ),
                authoritativeSource = "Philippine Red Cross / American Heart Association Guidelines",
                emergencyHotlineNumber = "143 / 911"
            ),

            // ==========================================
            // 1. CPR (FILIPINO)
            // ==========================================
            EmergencyGuide(
                id = "cpr-fil",
                category = GuideCategory.CPR,
                language = GuideLanguage.FILIPINO,
                title = "Hands-Only CPR (Pag-atake sa Puso at Pagkawala ng Malay)",
                summary = "Ang agarang pagdiin sa dibdib ay nagpapanatili ng daloy ng dugo sa utak at puso kapag ang biktima ay walang malay at hindi humihinga nang normal.",
                steps = listOf(
                    GuideStep(1, "Suriin ang Kamalayan at Paghinga", "Tapikin nang madiin ang balikat ng biktima at sumigaw: 'Ayos ka lang ba?'. Tingnan ang dibdib sa loob ng 5-10 segundo. Ang pagsinghap (gasping) ay hindi normal na paghinga.", "Kung walang malay at hindi humihinga, agad magpatulong tumawag sa 911 o Philippine Red Cross 143."),
                    GuideStep(2, "Ipuwesto ang mga Kamay", "Ilagay ang ibabang bahagi ng palad (heel of hand) sa gitna ng dibdib ng biktima. Ikapit ang kabilang kamay sa ibabaw nito. Panatilihing tuwid ang mga siko.", null),
                    GuideStep(3, "Madiin at Mabilis na Pagdiin", "Pumindot nang madiin at mabilis sa bilis na 100 hanggang 120 beats kada minuto. Idiin nang di-bababa sa 2 pulgada (5 cm) ang lalim.", "Hayaang bumalik nang buo ang dibdib bago muling idiin."),
                    GuideStep(4, "Ipagpatuloy Hanggang Dumating ang Saklolo", "Huwag ititigil ang pagdiin hangga't hindi dumarating ang ambulansya, handa nang gamitin ang AED, o muling nagkamalay ang pasyente.", null)
                ),
                whatNotToDo = listOf(
                    "HUWAG antalahin ang pagdiin sa dibdib kung ikaw ay hindi sinanay na mediko.",
                    "HUWAG sasandalan ang dibdib sa pagitan ng mga pagdiin; hayaang umangat muli ang dibdib.",
                    "HUWAG ihinto ang CPR nang higit sa 10 segundo."
                ),
                authoritativeSource = "Philippine Red Cross Safety Services",
                emergencyHotlineNumber = "143 / 911"
            ),

            // ==========================================
            // 2. SEVERE BLEEDING (ENGLISH)
            // ==========================================
            EmergencyGuide(
                id = "bleeding-en",
                category = GuideCategory.BLEEDING,
                language = GuideLanguage.ENGLISH,
                title = "Severe Bleeding & Wound Management",
                summary = "Uncontrolled severe bleeding can become fatal within 3 to 5 minutes. Direct sustained pressure is the primary life-saving action.",
                steps = listOf(
                    GuideStep(1, "Ensure Scene Safety & Expose Wound", "Ensure the environment is safe. Tear or cut clothing to expose the exact origin of bleeding.", null),
                    GuideStep(2, "Apply Direct Firm Pressure", "Press directly on the bleeding site with a sterile gauze, clean cloth, or your bare gloved hand with your full body weight.", "Maintain continuous pressure. Do not lift the cloth to check."),
                    GuideStep(3, "Pack Deep Wounds", "For deep puncture or laceration wounds in the groin or armpit, firmly pack the wound with clean cloth or gauze, then maintain hard pressure.", null),
                    GuideStep(4, "Apply Tourniquet if Limbs Bleed Uncontrollably", "For severe limb arterial bleeding that doesn't stop, apply a commercial tourniquet 2-3 inches above the wound (never over a joint). Tighten until bleeding stops.", "Note the exact time of tourniquet application.")
                ),
                whatNotToDo = listOf(
                    "DO NOT remove embedded objects (knives, glass) — stabilize them in place with rolled dressings.",
                    "DO NOT loosen a tourniquet once tightened; let emergency doctors release it at the trauma ward.",
                    "DO NOT remove soaked dressings; layer clean dressings directly on top."
                ),
                authoritativeSource = "Philippine Red Cross First Aid Manual / Stop the Bleed",
                emergencyHotlineNumber = "143 / 911"
            ),

            // ==========================================
            // 2. SEVERE BLEEDING (FILIPINO)
            // ==========================================
            EmergencyGuide(
                id = "bleeding-fil",
                category = GuideCategory.BLEEDING,
                language = GuideLanguage.FILIPINO,
                title = "Malubhang Pagdurugo at Pamamahala sa Sugat",
                summary = "Ang matinding pagdurugo ay maaaring nakamamatay sa loob ng 3 hanggang 5 minuto. Ang tuluy-tuloy na pagdiin sa sugat ang pinakamahalagang hakbang.",
                steps = listOf(
                    GuideStep(1, "Siguraduhing Ligtas ang Paligid", "Tiyaking ligtas ka at ang biktima. Gupitin o tanggalin ang damit sa ibabaw ng sugat upang makita ang pinanggagalingan ng dugo.", null),
                    GuideStep(2, "Direktang Pagdiin sa Sugat", "Diinan nang madiin gamit ang malinis na tela o gasa gamit ang bigat ng iyong katawan.", "Huwag iaangat ang tela para sumilip; panatilihin ang diin nang walang tigil."),
                    GuideStep(3, "Pagsiksik sa Malalim na Sugat", "Kung malalim ang sugat sa singit o kili-kili, isiksik nang mabuti ang malinis na gasa sa loob ng sugat at patuloy na diinan nang buong lakas.", null),
                    GuideStep(4, "Paggamit ng Tourniquet sa Brasong/Binting Dumudugo", "Kung sa braso o binti at sumasirit ang dugo, ilagay ang tourniquet 2-3 pulgada sa itaas ng sugat (huwag sa kasu-kasuan). Higpitan hanggang huminto ang dugo.", "Isulat ang eksaktong oras ng paglagay ng tourniquet.")
                ),
                whatNotToDo = listOf(
                    "HUWAG bubunutin ang nakatarak na bagay (kutsilyo, bubog) — suportahan lamang ito ng tela sa paligid.",
                    "HUWAG luluwagan ang tourniquet kapag naitali na; doktor lamang sa ospital ang magtatanggal nito.",
                    "HUWAG aalisin ang basang tela; magpatong lamang ng bagong malinis na tela sa ibabaw."
                ),
                authoritativeSource = "Philippine Red Cross / Kagawaran ng Kalusugan (DOH)",
                emergencyHotlineNumber = "143 / 911"
            ),

            // ==========================================
            // 3. CHOKING (ENGLISH)
            // ==========================================
            EmergencyGuide(
                id = "choking-en",
                category = GuideCategory.CHOKING,
                language = GuideLanguage.ENGLISH,
                title = "Choking First Aid (Conscious Adult/Child)",
                summary = "When airway obstruction prevents speaking or coughing, rapid back blows and abdominal thrusts are required.",
                steps = listOf(
                    GuideStep(1, "Assess Choking Severity", "Ask: 'Are you choking?'. If the person can cough forcefully, encourage them to cough. If they cannot speak, cry, or breathe, act immediately.", null),
                    GuideStep(2, "Deliver 5 Firm Back Blows", "Stand to the side and slightly behind. Support the victim's chest with one hand and lean them forward. Deliver 5 sharp blows between shoulder blades with the heel of your hand.", null),
                    GuideStep(3, "Perform 5 Abdominal Thrusts (Heimlich)", "Stand behind, wrap arms around waist. Make a fist with one hand just above the navel. Grasp fist with other hand and thrust inward and upward rapidly 5 times.", null),
                    GuideStep(4, "Repeat Cycle Until Cleared or Unresponsive", "Alternate 5 back blows and 5 abdominal thrusts. If victim becomes unconscious, lower to ground and begin CPR immediately.", "Always look in the mouth before giving rescue breaths if CPR is started.")
                ),
                whatNotToDo = listOf(
                    "DO NOT perform blind finger sweeps in the mouth, which may push foreign objects deeper into the trachea.",
                    "DO NOT slap a person on the back while they are standing upright, which can lodge the object further down."
                ),
                authoritativeSource = "Philippine Red Cross Emergency Services",
                emergencyHotlineNumber = "143 / 911"
            ),

            // ==========================================
            // 3. CHOKING (FILIPINO)
            // ==========================================
            EmergencyGuide(
                id = "choking-fil",
                category = GuideCategory.CHOKING,
                language = GuideLanguage.FILIPINO,
                title = "Pang-unang Lunas sa Pagkabulaukan (Nabulunan)",
                summary = "Kapag may nakabarang pagkain o bagay sa lalamunan at hindi na makapagsalita o makaubo, kailangan ng agarang hampas sa likod at diin sa tiyan.",
                steps = listOf(
                    GuideStep(1, "Suriin ang Biktima", "Tanungin: 'Nabulunan ka ba?'. Kung nakakaubo pa, hikayating umubo nang malakas. Kung hindi na makapagsalita o makahinga, kumilos agad.", null),
                    GuideStep(2, "5 Hampas sa Likod (Back Blows)", "Tumayo sa tagiliran at bahagyang likod ng biktima. Suportahan ang dibdib at iyukod siya paharap. Magbigay ng 5 madiing hampas sa pagitan ng mga paypay gamit ang palad.", null),
                    GuideStep(3, "5 Diin sa Tiyan (Heimlich Maneuver)", "Tumayo sa likod, ikapit ang braso sa baywang. Isara ang isang kamao sa itaas lamang ng pusod. Hawakan ang kamao gamit ang kabilang kamay at hilahin papasok at paitaas nang mabilis nang 5 beses.", null),
                    GuideStep(4, "Ulitin Hanggang Maalis o Mawalan ng Malay", "Ulitin ang 5 hampas sa likod at 5 diin sa tiyan. Kung nawalan ng malay, ihiga nang maingat sa sahig at agad simulan ang CPR.", null)
                ),
                whatNotToDo = listOf(
                    "HUWAG kakapain o susundutin ang bibig nang pabulag; maaaring maitulak paloob ang nakabara.",
                    "HUWAG hahampasin ang likod habang nakatayo nang tuwid ang biktima."
                ),
                authoritativeSource = "Philippine Red Cross Safety Services",
                emergencyHotlineNumber = "143 / 911"
            ),

            // ==========================================
            // 4. EARTHQUAKE (ENGLISH)
            // ==========================================
            EmergencyGuide(
                id = "quake-en",
                category = GuideCategory.EARTHQUAKE,
                language = GuideLanguage.ENGLISH,
                title = "Earthquake Protocol: Duck, Cover, and Hold",
                summary = "During seismic ground shaking, falling debris and shattered glass pose the highest risk of injury.",
                steps = listOf(
                    GuideStep(1, "DUCK (Drop)", "Drop immediately onto your hands and knees wherever you are. This protects you from being knocked over and allows you to stay low.", null),
                    GuideStep(2, "COVER", "Take cover underneath a sturdy desk or table. Protect your head and neck with your arms if no shelter is nearby. Move away from glass windows, heavy shelves, and hanging fixtures.", null),
                    GuideStep(3, "HOLD ON", "Hold firmly onto your shelter until shaking completely stops. Be prepared for your shelter to shift with ground vibrations.", null),
                    GuideStep(4, "Evacuate Safely After Shaking Stops", "Once tremors cease, exit via stairs. Never use elevators. Check for gas leaks, fires, and damaged electrical wires. Move to designated open assembly areas.", "Expect aftershocks. Stay alert.")
                ),
                whatNotToDo = listOf(
                    "DO NOT rush outside while the ground is actively shaking (falling masonry and wires cause the most casualties).",
                    "DO NOT use elevators during or immediately after an earthquake.",
                    "DO NOT use matches, lighters, or open flames indoors due to potential gas leaks."
                ),
                authoritativeSource = "PHIVOLCS (Philippine Institute of Volcanology and Seismology)",
                emergencyHotlineNumber = "911 / 117"
            ),

            // ==========================================
            // 4. EARTHQUAKE (FILIPINO)
            // ==========================================
            EmergencyGuide(
                id = "quake-fil",
                category = GuideCategory.EARTHQUAKE,
                language = GuideLanguage.FILIPINO,
                title = "Alituntunin sa Lindol: Duck, Cover, and Hold",
                summary = "Sa panahon ng lindol, ang mga bumabagsak na debris at nababasag na salamin ang pinakamalaking panganib.",
                steps = listOf(
                    GuideStep(1, "DUCK (Dumapa / Lumuhod)", "Agad na lumuhod at dumapa sa sahig upang hindi maitumba ng pagyanig.", null),
                    GuideStep(2, "COVER (Magtago / Takpan ang Ulo)", "Sumilong sa ilalim ng matibay na mesa. Takpan ang ulo at leeg gamit ang mga braso kung walang masisilungan. Lumayo sa mga bintanang salamin at mabibigat na gamit.", null),
                    GuideStep(3, "HOLD ON (Kumapit)", "Kumapit nang mahigpit sa paa ng mesa hanggang sa tuluyang huminto ang lindol.", null),
                    GuideStep(4, "Ligtas na Paglikas Pagkatapos ng Lindol", "Kapag huminto na ang pagyanig, lumabas gamit ang hagdanan. Huwag kailanman gagamit ng elevator. Pumunta sa bukas na ligtas na evacuation area.", "Mag-ingat sa mga aftershock o kasunod na pagyanig.")
                ),
                whatNotToDo = listOf(
                    "HUWAG mag-panic o tumakbo palabas habang lumilindol pa; marami ang nababagsakan ng debris.",
                    "HUWAG gumamit ng elevator sa panahon at pagkatapos ng lindol.",
                    "HUWAG magsisindi ng posporo o kandila dahil maaaring may tagas na gas (LPG)."
                ),
                authoritativeSource = "PHIVOLCS (DOST) / NDRRMC",
                emergencyHotlineNumber = "911 / 117"
            ),

            // ==========================================
            // 5. FIRE EVACUATION (ENGLISH)
            // ==========================================
            EmergencyGuide(
                id = "fire-en",
                category = GuideCategory.FIRE,
                language = GuideLanguage.ENGLISH,
                title = "Fire Evacuation & Stop, Drop, and Roll",
                summary = "Smoke inhalation is the primary cause of fire deaths. Toxic gases rise, leaving cleaner, cooler air near the floor.",
                steps = listOf(
                    GuideStep(1, "Crawl Low Under Smoke", "Stay on your hands and knees below the smoke line. Smoke and poisonous carbon monoxide rise to the ceiling.", null),
                    GuideStep(2, "Feel Closed Doors Before Opening", "Check doors with the back of your hand (knuckles) near the top and doorknob. If the door feels warm or hot, DO NOT open it; use an alternate exit.", null),
                    GuideStep(3, "Escape and Close Doors Behind You", "Close doors as you exit to slow the fire spread and oxygen supply. Never return inside a burning building for pets or belongings.", null),
                    GuideStep(4, "If Clothes Catch Fire: STOP, DROP, and ROLL", "Do not run! Immediately stop, drop to the ground, cover your face with your hands, and roll back and forth until the flames are smothered.", null)
                ),
                whatNotToDo = listOf(
                    "DO NOT use elevators during a fire evacuation.",
                    "DO NOT re-enter a burning structure under any circumstance.",
                    "DO NOT open hot doors."
                ),
                authoritativeSource = "Bureau of Fire Protection (BFP Hotline: 02-8426-0219)",
                emergencyHotlineNumber = "911 / 02-8426-0219"
            ),

            // ==========================================
            // 5. FIRE EVACUATION (FILIPINO)
            // ==========================================
            EmergencyGuide(
                id = "fire-fil",
                category = GuideCategory.FIRE,
                language = GuideLanguage.FILIPINO,
                title = "Paglikas sa Sunog at Stop, Drop, and Roll",
                summary = "Ang pagkalanghap ng usok ang pangunahing sanhi ng kamatayan sa sunog. Gumapang nang mababa dahil mas malinis ang hangin sa sahig.",
                steps = listOf(
                    GuideStep(1, "Gumapang nang Mababa sa Ilalim ng Usok", "Manatiling nakaluhod at gumapang sa sahig. Ang nakalalasong usok ay umaakyat paitaas.", null),
                    GuideStep(2, "Damhin ang Pinto Bago Buksan", "Gamitin ang likod ng palad para damhin ang pinto at hawakan. Kung mainit, HUWAG itong buksan at maghanap ng ibang labasan.", null),
                    GuideStep(3, "Lumabas at Isara ang Pinto sa Likod Mo", "Isara ang pinto paglabas upang mapabagal ang pagkalat ng apoy. Huwag nang bumalik para kumuha ng gamit.", null),
                    GuideStep(4, "Kapag Nagliyab ang Damit: STOP, DROP, at ROLL", "Huwag tatakbo! Huminto, dumapa sa lupa, takpan ang mukha gamit ang kamay, at magpagulong-gulong hanggang mamatay ang apoy.", null)
                ),
                whatNotToDo = listOf(
                    "HUWAG gagamit ng elevator habang may sunog.",
                    "HUWAG babalik sa loob ng nasusunog na gusali kahit anong mangyari.",
                    "HUWAG bubuksan ang mainit na pinto."
                ),
                authoritativeSource = "Bureau of Fire Protection (BFP)",
                emergencyHotlineNumber = "911 / 02-8426-0219"
            ),

            // ==========================================
            // 6. TYPHOON SAFETY (ENGLISH)
            // ==========================================
            EmergencyGuide(
                id = "typhoon-en",
                category = GuideCategory.TYPHOON,
                language = GuideLanguage.ENGLISH,
                title = "Typhoon & Storm Surge Preparedness",
                summary = "Tropical cyclones in the Philippines produce destructive winds, torrential rainfall, and coastal storm surges.",
                steps = listOf(
                    GuideStep(1, "Monitor PAGASA Tropical Cyclone Warnings", "Track public storm warning signals (Signal 1 to 5). Charge power banks, communication radios, and mobile devices.", null),
                    GuideStep(2, "Secure Property & Clear Drainage", "Fasten loose roof sheets, trim overhanging tree branches, and board large glass windows. Clear local gutters.", null),
                    GuideStep(3, "Heed Preemptive Evacuation Orders", "If living in low-lying, coastal, or landslide-prone zones, evacuate to designated LGU storm shelters before landfall.", "Do not wait for darkness or floodwaters to rise before evacuating."),
                    GuideStep(4, "Stay Indoors Away From Windows", "Stay in the strongest interior room of the house during the eye of the storm. Do not be deceived by the temporary lull.", null)
                ),
                whatNotToDo = listOf(
                    "DO NOT go outside when the calm eye of the typhoon passes over; violent reverse winds follow quickly.",
                    "DO NOT attempt to cross flooded roadways or swollen rivers.",
                    "DO NOT stay in coastal homes when storm surge warnings are in effect."
                ),
                authoritativeSource = "PAGASA (DOST) / NDRRMC",
                emergencyHotlineNumber = "911 / DPWH 165-02"
            ),

            // ==========================================
            // 6. TYPHOON SAFETY (FILIPINO)
            // ==========================================
            EmergencyGuide(
                id = "typhoon-fil",
                category = GuideCategory.TYPHOON,
                language = GuideLanguage.FILIPINO,
                title = "Paghahanda sa Bagyo at Daluyong ng Bagyo (Storm Surge)",
                summary = "Ang mga bagyo sa Pilipinas ay nagdadala ng malalakas na hangin, baha, at daluyong ng bagyo sa mga baybayin.",
                steps = listOf(
                    GuideStep(1, "Subaybayan ang Ulat ng PAGASA", "Alamin ang Storm Warning Signal (Signal 1 hanggang 5). I-charge ang mga cellphone, power bank, at flashlight.", null),
                    GuideStep(2, "Ihanda ang Bahay at 'Go Bag'", "Talian ang bubong, putulan ang mga sanga ng puno, at maghanda ng emergency food, tubig, at first aid kit para sa 72 oras.", null),
                    GuideStep(3, "Sumunod sa Preemptive Evacuation ng LGU", "Kung nakatira sa tabing-dagat o paanan ng bundok, agad lumikas bago pa man mag-landfall ang bagyo.", "Huwag nang hintaying tumaas ang tubig bago lumikas."),
                    GuideStep(4, "Manatili sa Loob ng Bahay", "Lumayo sa mga bintana. Kung dumaan ang 'mata ng bagyo' (eye), huwag lumabas dahil muling hahampas ang hangin sa kabilang direksyon.", null)
                ),
                whatNotToDo = listOf(
                    "HUWAG lumabas kapag humupa ang hangin sa gitna ng bagyo (mata ng bagyo).",
                    "HUWAG tatawid sa rumaragasang ilog o baha.",
                    "HUWAG mananatili sa baybayin kapag may babala ng storm surge."
                ),
                authoritativeSource = "PAGASA (DOST) / NDRRMC",
                emergencyHotlineNumber = "911 / DPWH 165-02"
            ),

            // ==========================================
            // 7. FLOOD SAFETY (ENGLISH)
            // ==========================================
            EmergencyGuide(
                id = "flood-en",
                category = GuideCategory.FLOOD,
                language = GuideLanguage.ENGLISH,
                title = "Flood Safety & Waterborne Hazard Prevention",
                summary = "Floods are the most common disaster in the Philippines. Submerged electrical hazards and leptospirosis pose severe health risks.",
                steps = listOf(
                    GuideStep(1, "Cut Main Electrical Breakers", "Switch off the main circuit breaker and unplug appliances before floodwaters enter living spaces.", "Never touch electrical equipment while standing in water."),
                    GuideStep(2, "Move to Higher Ground", "Relocate family members, essential medications, and emergency kits to the second floor or elevated community evacuation centers.", null),
                    GuideStep(3, "Avoid Wading in Floodwaters", "Floodwaters carry dangerous sewage, debris, and Leptospira bacteria from rat urine. Wear protective rubber boots.", null),
                    GuideStep(4, "Never Drive Through Flooded Streets", "6 inches of moving water can knock an adult down; 12 inches can sweep away a light vehicle. 'Turn Around, Don't Drown.'", null)
                ),
                whatNotToDo = listOf(
                    "DO NOT walk, swim, or drive through flowing floodwaters.",
                    "DO NOT drink tap or well water contaminated by floodwaters; boil for at least 3 minutes.",
                    "DO NOT touch downed electrical poles or submerged power cables."
                ),
                authoritativeSource = "Department of Health (DOH) / MMDA / PAGASA",
                emergencyHotlineNumber = "911 / DPWH 165-02"
            ),

            // ==========================================
            // 7. FLOOD SAFETY (FILIPINO)
            // ==========================================
            EmergencyGuide(
                id = "flood-fil",
                category = GuideCategory.FLOOD,
                language = GuideLanguage.FILIPINO,
                title = "Kaligtasan sa Baha at Pag-iwas sa Leptospirosis",
                summary = "Ang baha ay nagdudulot ng kuryente at nakamamatay na impeksyon gaya ng Leptospirosis mula sa ihi ng daga.",
                steps = listOf(
                    GuideStep(1, "Patayin ang Main Circuit Breaker", "I-switch off agad ang main breaker ng kuryente bago pa man pumasok ang tubig sa loob ng bahay.", "Huwag hahawak ng switch o kable habang nakatayo sa tubig."),
                    GuideStep(2, "Lumipat sa Mataas na Lugar", "Umakyat sa ikalawang palapag o pumunta sa itinalagang evacuation center kasama ang emergency kit.", null),
                    GuideStep(3, "Iwasang Lumusong sa Baha", "Magsuot ng bota. Kung may sugat, huwag lumusong dahil sa banta ng Leptospirosis. Uminom ng prophylaxis (Doxycycline) ayon sa payo ng health center.", null),
                    GuideStep(4, "Huwag Magmaneho sa Baha", "Kahit ang mababaw na rumaragasang tubig ay kayang magpatumba ng tao o magtangay ng sasakyan.", null)
                ),
                whatNotToDo = listOf(
                    "HUWAG hahayaang maglaro o lumangoy ang mga bata sa baha.",
                    "HUWAG iinumin ang tubig-gripo na posibleng kontaminado; pakuluan muna nang di-bababa sa 3 minuto.",
                    "HUWAG lalapit sa mga natumbang poste o nakalubog na kable ng kuryente."
                ),
                authoritativeSource = "Kagawaran ng Kalusugan (DOH) / NDRRMC",
                emergencyHotlineNumber = "911 / DPWH 165-02"
            )
        )
    }

    /**
     * Returns all offline emergency guides across all categories and languages.
     */
    fun getAllGuides(): List<EmergencyGuide> = cachedEmergencyGuidesList

    /**
     * Filters guides matching a specific language preference (English or Filipino).
     */
    fun getGuidesByLanguage(language: GuideLanguage): List<EmergencyGuide> {
        return cachedEmergencyGuidesList.filter { it.language == language }
    }

    /**
     * Retrieves an individual guide by category and language.
     */
    fun getGuidesByCategory(category: GuideCategory, language: GuideLanguage = GuideLanguage.ENGLISH): EmergencyGuide? {
        return cachedEmergencyGuidesList.firstOrNull { it.category == category && it.language == language }
    }

    /**
     * Performs fast offline keyword search across guide titles, summaries, and instructional steps.
     *
     * @param query Search keywords entered by the user
     * @param language Optional language filter (English or Filipino)
     */
    fun searchGuides(query: String, language: GuideLanguage? = null): List<EmergencyGuide> {
        val cleanedSearchQuery = query.trim().lowercase()
        return cachedEmergencyGuidesList.filter { emergencyGuide ->
            val isLanguageMatching = (language == null || emergencyGuide.language == language)
            val isTitleMatching = emergencyGuide.title.lowercase().contains(cleanedSearchQuery)
            val isSummaryMatching = emergencyGuide.summary.lowercase().contains(cleanedSearchQuery)
            val isStepInstructionMatching = emergencyGuide.steps.any { step ->
                step.title.lowercase().contains(cleanedSearchQuery) ||
                        step.instruction.lowercase().contains(cleanedSearchQuery)
            }

            isLanguageMatching && (isTitleMatching || isSummaryMatching || isStepInstructionMatching)
        }
    }
}
