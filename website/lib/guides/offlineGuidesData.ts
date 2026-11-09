export interface GuideStep {
  stepNumber: number;
  title: string;
  instruction: string;
  criticalNote?: string | null;
}

export interface EmergencyGuide {
  id: string;
  category: "cpr" | "bleeding" | "choking" | "earthquake" | "fire" | "typhoon" | "flood";
  language: "en" | "fil";
  title: string;
  summary: string;
  steps: GuideStep[];
  whatNotToDo: string[];
  authoritativeSource: string;
  emergencyHotlineNumber: string;
}

export const offlineGuides: EmergencyGuide[] = [
  // 1. CPR ENGLISH
  {
    id: "cpr-en",
    category: "cpr",
    language: "en",
    title: "Hands-Only CPR (Cardiac Arrest)",
    summary:
      "Immediate chest compressions maintain vital blood flow to the brain and heart when a person is unresponsive and not breathing normally.",
    steps: [
      {
        stepNumber: 1,
        title: "Check Responsiveness & Breathing",
        instruction:
          "Tap the victim's shoulders firmly and shout: 'Are you OK?'. Look at the chest for 5-10 seconds to check for normal breathing. Gasping is not normal breathing.",
        criticalNote:
          "If unresponsive and not breathing normally, shout for help and have someone call 911 or Red Cross 143 immediately.",
      },
      {
        stepNumber: 2,
        title: "Position Your Hands",
        instruction:
          "Place the heel of one hand in the center of the chest (lower half of breastbone). Interlock your other hand on top. Keep your elbows locked straight.",
      },
      {
        stepNumber: 3,
        title: "Compress Hard and Fast",
        instruction:
          "Push hard and fast in the center of the chest at 100 to 120 beats per minute (to the beat of 'Stayin' Alive'). Compress at least 2 inches (5 cm) deep.",
        criticalNote: "Allow complete chest recoil between compressions without lifting hands off.",
      },
      {
        stepNumber: 4,
        title: "Continue Until Relief Arrives",
        instruction:
          "Do not stop compressions until trained medical personnel arrive, an AED is ready to use, or the person clearly recovers consciousness.",
      },
    ],
    whatNotToDo: [
      "DO NOT delay compressions to check for a pulse if you are an untrained lay responder.",
      "DO NOT lean on the victim's chest between compressions; allow the chest to fully rise.",
      "DO NOT interrupt compressions for more than 10 seconds.",
    ],
    authoritativeSource: "Philippine Red Cross / American Heart Association Guidelines",
    emergencyHotlineNumber: "143 / 911",
  },
  // 1. CPR FILIPINO
  {
    id: "cpr-fil",
    category: "cpr",
    language: "fil",
    title: "Hands-Only CPR (Pag-atake sa Puso at Pagkawala ng Malay)",
    summary:
      "Ang agarang pagdiin sa dibdib ay nagpapanatili ng daloy ng dugo sa utak at puso kapag ang biktima ay walang malay at hindi humihinga nang normal.",
    steps: [
      {
        stepNumber: 1,
        title: "Suriin ang Kamalayan at Paghinga",
        instruction:
          "Tapikin nang madiin ang balikat ng biktima at sumigaw: 'Ayos ka lang ba?'. Tingnan ang dibdib sa loob ng 5-10 segundo. Ang pagsinghap (gasping) ay hindi normal na paghinga.",
        criticalNote:
          "Kung walang malay at hindi humihinga, agad magpatulong tumawag sa 911 o Philippine Red Cross 143.",
      },
      {
        stepNumber: 2,
        title: "Ipuwesto ang mga Kamay",
        instruction:
          "Ilagay ang ibabang bahagi ng palad (heel of hand) sa gitna ng dibdib ng biktima. Ikapit ang kabilang kamay sa ibabaw nito. Panatilihing tuwid ang mga siko.",
      },
      {
        stepNumber: 3,
        title: "Madiin at Mabilis na Pagdiin",
        instruction:
          "Pumindot nang madiin at mabilis sa bilis na 100 hanggang 120 beats kada minuto. Idiin nang di-bababa sa 2 pulgada (5 cm) ang lalim.",
        criticalNote: "Hayaang bumalik nang buo ang dibdib bago muling idiin.",
      },
      {
        stepNumber: 4,
        title: "Ipagpatuloy Hanggang Dumating ang Saklolo",
        instruction:
          "Huwag ititigil ang pagdiin hangga't hindi dumarating ang ambulansya, handa nang gamitin ang AED, o muling nagkamalay ang pasyente.",
      },
    ],
    whatNotToDo: [
      "HUWAG antalahin ang pagdiin sa dibdib kung ikaw ay hindi sinanay na mediko.",
      "HUWAG sasandalan ang dibdib sa pagitan ng mga pagdiin; hayaang umangat muli ang dibdib.",
      "HUWAG ihinto ang CPR nang higit sa 10 segundo.",
    ],
    authoritativeSource: "Philippine Red Cross Safety Services",
    emergencyHotlineNumber: "143 / 911",
  },
  // 2. BLEEDING ENGLISH
  {
    id: "bleeding-en",
    category: "bleeding",
    language: "en",
    title: "Severe Bleeding & Wound Management",
    summary:
      "Uncontrolled severe bleeding can become fatal within 3 to 5 minutes. Direct sustained pressure is the primary life-saving action.",
    steps: [
      {
        stepNumber: 1,
        title: "Ensure Scene Safety & Expose Wound",
        instruction:
          "Ensure the environment is safe. Tear or cut clothing to expose the exact origin of bleeding.",
      },
      {
        stepNumber: 2,
        title: "Apply Direct Firm Pressure",
        instruction:
          "Press directly on the bleeding site with sterile gauze, clean cloth, or gloved hand with your full body weight.",
        criticalNote: "Maintain continuous pressure. Do not lift the cloth to check.",
      },
      {
        stepNumber: 3,
        title: "Pack Deep Wounds",
        instruction:
          "For deep puncture or laceration wounds in the groin or armpit, firmly pack the wound with clean cloth or gauze, then maintain hard pressure.",
      },
      {
        stepNumber: 4,
        title: "Apply Tourniquet if Limbs Bleed Uncontrollably",
        instruction:
          "For severe limb arterial bleeding that doesn't stop, apply a commercial tourniquet 2-3 inches above the wound (never over a joint). Tighten until bleeding stops.",
        criticalNote: "Note the exact time of tourniquet application.",
      },
    ],
    whatNotToDo: [
      "DO NOT remove embedded objects (knives, glass) — stabilize them in place with rolled dressings.",
      "DO NOT loosen a tourniquet once tightened; let emergency doctors release it at the trauma ward.",
      "DO NOT remove soaked dressings; layer clean dressings directly on top.",
    ],
    authoritativeSource: "Philippine Red Cross First Aid Manual / Stop the Bleed",
    emergencyHotlineNumber: "143 / 911",
  },
  // 2. BLEEDING FILIPINO
  {
    id: "bleeding-fil",
    category: "bleeding",
    language: "fil",
    title: "Malubhang Pagdurugo at Pamamahala sa Sugat",
    summary:
      "Ang matinding pagdurugo ay maaaring nakamamatay sa loob ng 3 hanggang 5 minuto. Ang tuluy-tuloy na pagdiin sa sugat ang pinakamahalagang hakbang.",
    steps: [
      {
        stepNumber: 1,
        title: "Siguraduhing Ligtas ang Paligid",
        instruction:
          "Tiyaking ligtas ka at ang biktima. Gupitin o tanggalin ang damit sa ibabaw ng sugat upang makita ang pinanggagalingan ng dugo.",
      },
      {
        stepNumber: 2,
        title: "Direktang Pagdiin sa Sugat",
        instruction:
          "Diinan nang madiin gamit ang malinis na tela o gasa gamit ang bigat ng iyong katawan.",
        criticalNote: "Huwag iaangat ang tela para sumilip; panatilihin ang diin nang walang tigil.",
      },
      {
        stepNumber: 3,
        title: "Pagsiksik sa Malalim na Sugat",
        instruction:
          "Kung malalim ang sugat sa singit o kili-kili, isiksik nang mabuti ang malinis na gasa sa loob ng sugat at patuloy na diinan nang buong lakas.",
      },
      {
        stepNumber: 4,
        title: "Paggamit ng Tourniquet sa Brasong/Binting Dumudugo",
        instruction:
          "Kung sa braso o binti at sumasirit ang dugo, ilagay ang tourniquet 2-3 pulgada sa itaas ng sugat (huwag sa kasu-kasuan). Higpitan hanggang huminto ang dugo.",
        criticalNote: "Isulat ang eksaktong oras ng paglagay ng tourniquet.",
      },
    ],
    whatNotToDo: [
      "HUWAG bubunutin ang nakatarak na bagay — suportahan lamang ito ng tela sa paligid.",
      "HUWAG luluwagan ang tourniquet kapag naitali na; doktor lamang sa ospital ang magtatanggal nito.",
      "HUWAG aalisin ang basang tela; magpatong lamang ng bagong malinis na tela sa ibabaw.",
    ],
    authoritativeSource: "Philippine Red Cross / Kagawaran ng Kalusugan (DOH)",
    emergencyHotlineNumber: "143 / 911",
  },
  // 3. EARTHQUAKE ENGLISH
  {
    id: "earthquake-en",
    category: "earthquake",
    language: "en",
    title: "Earthquake Safety: Duck, Cover, Hold",
    summary:
      "Earthquakes strike without warning. Protecting your head and torso from falling debris is the essential life-safety action during ground shaking.",
    steps: [
      {
        stepNumber: 1,
        title: "DROP (DUCK) to Your Hands and Knees",
        instruction:
          "Drop immediately before the earthquake knocks you down. Crawl under sturdy shelter.",
      },
      {
        stepNumber: 2,
        title: "COVER Your Head and Neck",
        instruction:
          "Take cover underneath a sturdy desk or table. If no shelter is nearby, cover your head and neck with your arms against an interior wall.",
      },
      {
        stepNumber: 3,
        title: "HOLD ON Until Shaking Stops",
        instruction:
          "Hold on to your shelter with one hand and be prepared to move with it until the shaking ceases completely.",
      },
      {
        stepNumber: 4,
        title: "Evacuate Calmly to Open Ground",
        instruction:
          "Once shaking stops, check for fire and gas leaks, put on shoes, and evacuate via stairs. Avoid glass facades and overhead power lines.",
        criticalNote: "Never use elevators during or immediately after an earthquake.",
      },
    ],
    whatNotToDo: [
      "DO NOT stand in a doorway; doorways are no stronger than any other part of modern buildings.",
      "DO NOT run outside while the ground is shaking; falling debris causes the majority of injuries.",
      "DO NOT use elevators.",
    ],
    authoritativeSource: "PHIVOLCS (DOST) / NDRRMC Guidelines",
    emergencyHotlineNumber: "911 / (02) 8426-1468",
  },
  // 4. TYPHOON ENGLISH
  {
    id: "typhoon-en",
    category: "typhoon",
    language: "en",
    title: "Typhoon & Tropical Storm Preparedness",
    summary:
      "The Philippines faces over 20 tropical cyclones yearly. Early evacuation, storm surge awareness, and emergency kit readiness prevent casualties.",
    steps: [
      {
        stepNumber: 1,
        title: "Monitor PAGASA Signal Warnings",
        instruction:
          "Check Tropical Cyclone Wind Signals (TCWS #1 to #5) and storm surge warnings from official government advisories.",
      },
      {
        stepNumber: 2,
        title: "Heed Preemptive Evacuation Orders",
        instruction:
          "If living in coastal lowlands, near riverbanks, or on landslide-prone mountain slopes, evacuate immediately when your LGU calls for preemptive action.",
      },
      {
        stepNumber: 3,
        title: "Secure Your Residence & Go-Bag",
        instruction:
          "Board up windows, secure loose outdoor objects, switch off the main electrical breaker if flooding threatens, and grab your 72-hour Go-Bag.",
      },
      {
        stepNumber: 4,
        title: "Beware the Eye of the Storm",
        instruction:
          "Do not be deceived by sudden calm weather when the storm's eye passes. Destructive counter-winds will resume violently within minutes.",
      },
    ],
    whatNotToDo: [
      "DO NOT attempt to cross flooded roads on foot or by vehicle.",
      "DO NOT touch downed electrical wires; treat all wires as energized.",
      "DO NOT ignore local evacuation warnings to guard property.",
    ],
    authoritativeSource: "PAGASA (DOST) / NDRRMC Advisories",
    emergencyHotlineNumber: "911 / (02) 8284-0800",
  },
];
