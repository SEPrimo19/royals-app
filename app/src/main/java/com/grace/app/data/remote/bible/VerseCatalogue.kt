package com.grace.app.data.remote.bible

import java.time.LocalDate
import kotlin.random.Random

object VerseCatalogue {

    enum class Season { GENERAL, CHRISTMAS, HOLY_WEEK, PENTECOST }

    private data class CatalogueVerse(val ref: String, val season: Season)

    private val kjvText: Map<String, String> = mapOf(
        "John 3:16" to "For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.",
        "Jeremiah 29:11" to "For I know the thoughts that I think toward you, saith the Lord, thoughts of peace, and not of evil, to give you an expected end.",
        "Philippians 4:13" to "I can do all things through Christ which strengtheneth me.",
        "Romans 8:28" to "And we know that all things work together for good to them that love God, to them who are the called according to his purpose.",
        "Psalm 23:1" to "The Lord is my shepherd; I shall not want.",
        "Proverbs 3:5-6" to "Trust in the Lord with all thine heart; and lean not unto thine own understanding. In all thy ways acknowledge him, and he shall direct thy paths.",
        "Isaiah 41:10" to "Fear thou not; for I am with thee: be not dismayed; for I am thy God: I will strengthen thee; yea, I will help thee; yea, I will uphold thee with the right hand of my righteousness.",
        "Matthew 6:33" to "But seek ye first the kingdom of God, and his righteousness; and all these things shall be added unto you.",
        "Joshua 1:9" to "Have not I commanded thee? Be strong and of a good courage; be not afraid, neither be thou dismayed: for the Lord thy God is with thee whithersoever thou goest.",
        "Psalm 46:10" to "Be still, and know that I am God: I will be exalted among the heathen, I will be exalted in the earth.",
        "Romans 12:2" to "And be not conformed to this world: but be ye transformed by the renewing of your mind, that ye may prove what is that good, and acceptable, and perfect, will of God.",
        "2 Corinthians 5:17" to "Therefore if any man be in Christ, he is a new creature: old things are passed away; behold, all things are become new.",
        "Galatians 2:20" to "I am crucified with Christ: nevertheless I live; yet not I, but Christ liveth in me: and the life which I now live in the flesh I live by the faith of the Son of God, who loved me, and gave himself for me.",
        "Ephesians 2:8-9" to "For by grace are ye saved through faith; and that not of yourselves: it is the gift of God: Not of works, lest any man should boast.",
        "Hebrews 11:1" to "Now faith is the substance of things hoped for, the evidence of things not seen.",
        "James 1:2-3" to "My brethren, count it all joy when ye fall into divers temptations; Knowing this, that the trying of your faith worketh patience.",
        "1 Peter 5:7" to "Casting all your care upon him; for he careth for you.",
        "Matthew 11:28-29" to "Come unto me, all ye that labour and are heavy laden, and I will give you rest. Take my yoke upon you, and learn of me; for I am meek and lowly in heart: and ye shall find rest unto your souls.",
        "Lamentations 3:22-23" to "It is of the Lord's mercies that we are not consumed, because his compassions fail not. They are new every morning: great is thy faithfulness.",
        "Psalm 27:1" to "The Lord is my light and my salvation; whom shall I fear? the Lord is the strength of my life; of whom shall I be afraid?",
        "Psalm 34:8" to "O taste and see that the Lord is good: blessed is the man that trusteth in him.",
        "Psalm 91:1-2" to "He that dwelleth in the secret place of the most High shall abide under the shadow of the Almighty. I will say of the Lord, He is my refuge and my fortress: my God; in him will I trust.",
        "Psalm 119:105" to "Thy word is a lamp unto my feet, and a light unto my path.",
        "Isaiah 40:31" to "But they that wait upon the Lord shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint.",
        "Micah 6:8" to "He hath shewed thee, O man, what is good; and what doth the Lord require of thee, but to do justly, and to love mercy, and to walk humbly with thy God?",
        "Zephaniah 3:17" to "The Lord thy God in the midst of thee is mighty; he will save, he will rejoice over thee with joy; he will rest in his love, he will joy over thee with singing.",
        "John 14:6" to "Jesus saith unto him, I am the way, the truth, and the life: no man cometh unto the Father, but by me.",
        "John 15:5" to "I am the vine, ye are the branches: He that abideth in me, and I in him, the same bringeth forth much fruit: for without me ye can do nothing.",
        "Romans 5:8" to "But God commendeth his love toward us, in that, while we were yet sinners, Christ died for us.",
        "Romans 10:9" to "That if thou shalt confess with thy mouth the Lord Jesus, and shalt believe in thine heart that God hath raised him from the dead, thou shalt be saved.",
        "1 Corinthians 13:4-7" to "Charity suffereth long, and is kind; charity envieth not; charity vaunteth not itself, is not puffed up, Doth not behave itself unseemly, seeketh not her own, is not easily provoked, thinketh no evil; Rejoiceth not in iniquity, but rejoiceth in the truth; Beareth all things, believeth all things, hopeth all things, endureth all things.",
        "Galatians 5:22-23" to "But the fruit of the Spirit is love, joy, peace, longsuffering, gentleness, goodness, faith, Meekness, temperance: against such there is no law.",
        "Ephesians 4:32" to "And be ye kind one to another, tenderhearted, forgiving one another, even as God for Christ's sake hath forgiven you.",
        "Philippians 4:6-7" to "Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God. And the peace of God, which passeth all understanding, shall keep your hearts and minds through Christ Jesus.",
        "Colossians 3:23" to "And whatsoever ye do, do it heartily, as to the Lord, and not unto men;",
        "2 Timothy 1:7" to "For God hath not given us the spirit of fear; but of power, and of love, and of a sound mind.",
        "Hebrews 12:1-2" to "Wherefore seeing we also are compassed about with so great a cloud of witnesses, let us lay aside every weight, and the sin which doth so easily beset us, and let us run with patience the race that is set before us, Looking unto Jesus the author and finisher of our faith; who for the joy that was set before him endured the cross, despising the shame, and is set down at the right hand of the throne of God.",
        "James 4:7" to "Submit yourselves therefore to God. Resist the devil, and he will flee from you.",
        "1 John 1:9" to "If we confess our sins, he is faithful and just to forgive us our sins, and to cleanse us from all unrighteousness.",
        "1 John 4:19" to "We love him, because he first loved us.",
        "Revelation 21:4" to "And God shall wipe away all tears from their eyes; and there shall be no more death, neither sorrow, nor crying, neither shall there be any more pain: for the former things are passed away.",
        "Psalm 139:14" to "I will praise thee; for I am fearfully and wonderfully made: marvellous are thy works; and that my soul knoweth right well.",
        "Matthew 5:14-16" to "Ye are the light of the world. A city that is set on an hill cannot be hid. Neither do men light a candle, and put it under a bushel, but on a candlestick; and it giveth light unto all that are in the house. Let your light so shine before men, that they may see your good works, and glorify your Father which is in heaven.",
        "Matthew 28:19-20" to "Go ye therefore, and teach all nations, baptizing them in the name of the Father, and of the Son, and of the Holy Ghost: Teaching them to observe all things whatsoever I have commanded you: and, lo, I am with you alway, even unto the end of the world. Amen.",
        "Mark 12:30-31" to "And thou shalt love the Lord thy God with all thy heart, and with all thy soul, and with all thy mind, and with all thy strength: this is the first commandment. And the second is like, namely this, Thou shalt love thy neighbour as thyself. There is none other commandment greater than these.",
        "Luke 1:37" to "For with God nothing shall be impossible.",
        "Acts 1:8" to "But ye shall receive power, after that the Holy Ghost is come upon you: and ye shall be witnesses unto me both in Jerusalem, and in all Judaea, and in Samaria, and unto the uttermost part of the earth.",
        "Romans 1:16" to "For I am not ashamed of the gospel of Christ: for it is the power of God unto salvation to every one that believeth; to the Jew first, and also to the Greek.",
        "Hebrews 13:5" to "Let your conversation be without covetousness; and be content with such things as ye have: for he hath said, I will never leave thee, nor forsake thee.",
        "Isaiah 53:5" to "But he was wounded for our transgressions, he was bruised for our iniquities: the chastisement of our peace was upon him; and with his stripes we are healed.",

        "Luke 2:10-11" to "And the angel said unto them, Fear not: for, behold, I bring you good tidings of great joy, which shall be to all people. For unto you is born this day in the city of David a Saviour, which is Christ the Lord.",
        "Isaiah 9:6" to "For unto us a child is born, unto us a son is given: and the government shall be upon his shoulder: and his name shall be called Wonderful, Counsellor, The mighty God, The everlasting Father, The Prince of Peace.",
        "Matthew 1:21" to "And she shall bring forth a son, and thou shalt call his name JESUS: for he shall save his people from their sins.",
        "Micah 5:2" to "But thou, Bethlehem Ephratah, though thou be little among the thousands of Judah, yet out of thee shall he come forth unto me that is to be ruler in Israel; whose goings forth have been from of old, from everlasting.",
        "John 1:14" to "And the Word was made flesh, and dwelt among us, (and we beheld his glory, the glory as of the only begotten of the Father,) full of grace and truth.",
        "Luke 2:14" to "Glory to God in the highest, and on earth peace, good will toward men.",
        "Galatians 4:4-5" to "But when the fulness of the time was come, God sent forth his Son, made of a woman, made under the law, To redeem them that were under the law, that we might receive the adoption of sons.",
        "Matthew 2:10-11" to "When they saw the star, they rejoiced with exceeding great joy. And when they were come into the house, they saw the young child with Mary his mother, and fell down, and worshipped him: and when they had opened their treasures, they presented unto him gifts; gold, and frankincense, and myrrh.",
        "Isaiah 7:14" to "Therefore the Lord himself shall give you a sign; Behold, a virgin shall conceive, and bear a son, and shall call his name Immanuel.",
        "John 3:17" to "For God sent not his Son into the world to condemn the world; but that the world through him might be saved.",

        "Mark 11:9-10" to "And they that went before, and they that followed, cried, saying, Hosanna; Blessed is he that cometh in the name of the Lord: Blessed be the kingdom of our father David, that cometh in the name of the Lord: Hosanna in the highest.",
        "John 13:34-35" to "A new commandment I give unto you, That ye love one another; as I have loved you, that ye also love one another. By this shall all men know that ye are my disciples, if ye have love one to another.",
        "Psalm 22:1" to "My God, my God, why hast thou forsaken me? why art thou so far from helping me, and from the words of my roaring?",
        "John 19:30" to "When Jesus therefore had received the vinegar, he said, It is finished: and he bowed his head, and gave up the ghost.",
        "Matthew 27:54" to "Now when the centurion, and they that were with him, watching Jesus, saw the earthquake, and those things that were done, they feared greatly, saying, Truly this was the Son of God.",
        "Matthew 28:6" to "He is not here: for he is risen, as he said. Come, see the place where the Lord lay.",
        "Romans 6:4" to "Therefore we are buried with him by baptism into death: that like as Christ was raised up from the dead by the glory of the Father, even so we also should walk in newness of life.",
        "1 Corinthians 15:55-57" to "O death, where is thy sting? O grave, where is thy victory? The sting of death is sin; and the strength of sin is the law. But thanks be to God, which giveth us the victory through our Lord Jesus Christ.",

        "Acts 2:1-4" to "And when the day of Pentecost was fully come, they were all with one accord in one place. And suddenly there came a sound from heaven as of a rushing mighty wind, and it filled all the house where they were sitting. And there appeared unto them cloven tongues like as of fire, and it sat upon each of them. And they were all filled with the Holy Ghost, and began to speak with other tongues, as the Spirit gave them utterance.",
        "Acts 2:38" to "Then Peter said unto them, Repent, and be baptized every one of you in the name of Jesus Christ for the remission of sins, and ye shall receive the gift of the Holy Ghost.",
        "John 14:26" to "But the Comforter, which is the Holy Ghost, whom the Father will send in my name, he shall teach you all things, and bring all things to your remembrance, whatsoever I have said unto you.",
        "Joel 2:28" to "And it shall come to pass afterward, that I will pour out my spirit upon all flesh; and your sons and your daughters shall prophesy, your old men shall dream dreams, your young men shall see visions:",
        "Ephesians 1:13-14" to "In whom ye also trusted, after that ye heard the word of truth, the gospel of your salvation: in whom also after that ye believed, ye were sealed with that holy Spirit of promise, Which is the earnest of our inheritance until the redemption of the purchased possession, unto the praise of his glory."
    )

    private val reflectionsAndPrayers: Map<String, Pair<String, String>> = mapOf(
        "John 3:16" to (
            "This is the gospel in one sentence: God loved YOU enough to give His own Son so you could have eternal life. Not because you earned it, not because you cleaned up first — just because He loves. Whoever believes in Jesus is rescued. Today, sit with that love and let it answer whatever you've been telling yourself about your worth." to
            "Father, thank You for loving me before I ever loved You back. Thank You for giving Jesus so I could live forever with You. Help me to believe deeply, not just nod my head. In Jesus' name, amen."
        ),
        "Jeremiah 29:11" to (
            "God's plans for you are not built around your fear — they're built around peace and a future. The Israelites heard this verse when life looked hopeless: exiled, no home, no obvious way forward. God's promise still held. Whatever waiting season you're in right now, He hasn't forgotten what He's writing for you." to
            "Lord, I confess I'm impatient. I want to see the full plan now, not trust You step by step. Quiet my anxious thoughts and help me believe You're working even when I can't see it. In Jesus' name, amen."
        ),
        "Philippians 4:13" to (
            "Paul wrote this from a prison cell — not from a stage. The strength he's talking about isn't a hype-up motivational quote; it's the supernatural endurance Christ gives when life is HARD. You can do whatever God has actually called you to do today, because the same Christ who strengthened Paul lives in you. The strength is His, not yours." to
            "Jesus, I'm tempted to rely on my own energy, my own willpower. Today I lay that down. Be my strength in the hard conversation, the boring task, the thing I'd rather avoid. In Your name, amen."
        ),
        "Romans 8:28" to (
            "God doesn't say everything that happens IS good. He says He WORKS all things — even the painful, unfair ones — together for good in those who love Him. Like puzzle pieces that make no sense alone but form a picture when assembled. Today's confusion may be tomorrow's clarity." to
            "Father, I trust You with the things I don't understand. The disappointment, the delay, the thing I wish hadn't happened. Weave them into something good in Your timing. In Jesus' name, amen."
        ),
        "Psalm 23:1" to (
            "The Lord is YOUR shepherd. Not a distant boss, not a hands-off creator — a shepherd who knows your name, your weaknesses, where you tend to wander. 'I shall not want' doesn't mean you'll never lack anything; it means the One leading you is enough. Today, follow the Shepherd's voice instead of the noise." to
            "Good Shepherd, thank You for not leaving me to figure life out alone. Lead me today — to the still waters when I'm anxious, to the green pastures when I'm exhausted. I'm Yours. In Jesus' name, amen."
        ),
        "Proverbs 3:5-6" to (
            "'Lean not on your own understanding' isn't an insult to your intelligence — it's an invitation to a bigger one. Your understanding sees what's in front of you; God's sees what you can't yet see. Trust + acknowledge Him in your real decisions today (not just the spiritual ones), and watch Him direct your path." to
            "Father, I'm tempted to figure it all out on my own. Today I'm choosing to bring You into the small decisions, not just the crisis ones. Direct my steps. In Jesus' name, amen."
        ),
        "Isaiah 41:10" to (
            "God repeats Himself here for a reason: 'Fear not… be not dismayed.' He knows we forget. The promise isn't that hard things won't come — it's that you won't face them alone. He'll strengthen you, help you, hold you up. Whatever you're afraid of today, you're not facing it solo." to
            "Lord, You see the fear I'm carrying that I haven't even named yet. Replace it with Your peace. Hold me up today when I feel like I might collapse. In Jesus' name, amen."
        ),
        "Matthew 6:33" to (
            "Jesus said this in the middle of a passage about worrying over food, clothes, and stuff. His answer to anxiety wasn't 'try harder' — it was 'reorder your priorities.' Put God's kingdom first, and the daily things you stress about start finding their proper place. Today, what would 'seeking first' look like in one concrete choice?" to
            "Jesus, my heart drifts to the things I want, the future I'm building for myself. Help me to seek Your kingdom and Your righteousness first today — and trust You with the rest. In Your name, amen."
        ),
        "Joshua 1:9" to (
            "God didn't tell Joshua to be brave because the danger was small — He said it because the assignment was HUGE. Joshua was leading a whole nation into unknown territory. Whatever territory God's leading you into today (a new school year, a hard conversation, a calling that scares you), the same promise applies: 'I am with you wherever you go.'" to
            "Lord, give me courage — not because I'm strong, but because You're with me. Where I want to run away today, help me to stand. Where I want to stay safe, help me to step forward. In Jesus' name, amen."
        ),
        "Psalm 46:10" to (
            "'Be still' isn't a suggestion to relax — in the original language it's a command to stop striving, to let go. The psalm before this verse describes mountains falling and waters roaring. In the chaos, God says: STOP. Know that I am God. Your job today isn't to fix everything; it's to be still and trust the One who already has." to
            "Father, my mind won't stop. The to-do list, the worries, the comparisons — they're all loud. Quiet me. Help me sit in Your presence and remember You are God and I am not. In Jesus' name, amen."
        ),
        "Romans 12:2" to (
            "The world has a default setting it tries to install in you: what success looks like, who matters, what's worth chasing. God says: don't be conformed — be transformed. The change happens through renewed thinking, fed by His Word over time. Every Bible reading, every prayer, every truth you choose to believe is another download of His pattern over the world's." to
            "Father, I confess I've let the world shape me more than I've let You. Renew my mind today. Show me where I'm believing a lie, and replace it with Your truth. In Jesus' name, amen."
        ),
        "2 Corinthians 5:17" to (
            "When you came to Christ, He didn't just forgive your past — He started a whole new YOU. The old version (the patterns, the labels, the shame) doesn't get to define you anymore. You may still wrestle with old habits, but you're no longer that old creature. You're new. Believe what God says about you today, not what your past says." to
            "Jesus, thank You for making me new. When the old voices try to define me, remind me of who I am in You. Help me to walk like the new creation I actually am. In Your name, amen."
        ),
        "Galatians 2:20" to (
            "Paul says he's been crucified with Christ — yet here he is, alive. How? The old self-running-the-show life died at the cross; the new Christ-lives-in-me life began. Your faith isn't a list of rules to keep; it's a person living inside you who loved you enough to die. Let Him live through you today." to
            "Jesus, I want less of me and more of You. Live through me today — through my words, my reactions, my decisions. I gave You my life; help me to actually surrender it. In Your name, amen."
        ),
        "Ephesians 2:8-9" to (
            "Salvation is a GIFT. You can't earn it by being good enough, religious enough, or sincere enough. Jesus paid for it; you receive it by faith. This kills both arrogance ('I deserve heaven because I'm good') and despair ('I'll never be good enough'). It's grace from start to finish, and grace can be received but never deserved." to
            "Father, thank You for the gift of grace. Forgive me when I try to earn what You already gave freely. Today I receive Your gift again — and I want to live grateful, not anxious. In Jesus' name, amen."
        ),
        "Hebrews 11:1" to (
            "Faith isn't pretending you can see something you can't. It's the SUBSTANCE of what you hope for — solid ground under feet that haven't reached the destination yet. The chapter that follows lists ordinary people who chose to trust God in the dark. You're being added to that list, one decision at a time." to
            "Father, increase my faith. Where I'm hoping for something I can't yet see, help me to stand on Your promises instead of my feelings. In Jesus' name, amen."
        ),
        "James 1:2-3" to (
            "'Count it joy' when life falls apart? James isn't denying the pain — he's giving you a longer view. Trials are the workshop where God produces endurance, and endurance is what you'll need for the long faith-journey ahead. The trial you're in right now isn't pointless; it's training." to
            "Lord, I don't naturally see joy in hard seasons — I see pain. Help me trust that You're producing something in me through this. Build endurance, not bitterness. In Jesus' name, amen."
        ),
        "1 Peter 5:7" to (
            "God doesn't say 'try not to worry.' He says 'cast' — hurl, fling — your anxieties onto Him. Why? Because He cares about you. Not just in general; specifically. The thing on your mind right now matters to Him. Today, name one worry and hand it to Him, out loud if you can." to
            "Father, here's what's been weighing on me: ____. I cast it on You. Thank You for caring about every detail. Help me leave it with You instead of picking it back up. In Jesus' name, amen."
        ),
        "Matthew 11:28-29" to (
            "Jesus' invitation is for the tired ones — the heavy laden, the burned out, the spiritually exhausted. He doesn't offer a vacation; He offers a yoke. A yoke is for working — but working WITH Him instead of trying to pull the load alone. His way is gentle. His yoke fits." to
            "Jesus, I'm tired in ways I haven't told anyone. I come to You today. Trade my heavy burdens for Your easy yoke. Teach me Your gentle rhythm. In Your name, amen."
        ),
        "Lamentations 3:22-23" to (
            "Jeremiah wrote this while watching his city burn. In the worst chapter of his life he said: God's mercies are NEW every morning. Not recycled, not running out. Whatever you got wrong yesterday, today's mercy is freshly delivered. Receive it like the morning sun — it's there for you." to
            "Father, thank You for mercy that's brand new today. I receive Your forgiveness for yesterday's failures and Your strength for today's challenges. Great is Your faithfulness. In Jesus' name, amen."
        ),
        "Psalm 27:1" to (
            "If the Lord is your light, the darkness has no real power over you. If He's the strength of your life, then the things you used to fear have lost their grip. Notice David asks 'whom shall I fear?' as a rhetorical question — he already knows the answer. Today, walk in that same confidence." to
            "Lord, You are my light. Drive out the fears I've been letting live in my head. You're stronger than any of them. I trust You. In Jesus' name, amen."
        ),
        "Psalm 34:8" to (
            "'Taste and see' isn't a metaphor for theory — it's an invitation to EXPERIENCE God's goodness for yourself. You can't know how good food is by reading about it; you have to taste. Same with God. Spend time in His presence today; obey something He's been nudging you about. You'll see — He's good." to
            "Father, I want to know You by experience, not just by hearsay. Show me Your goodness today in a way I can taste and see. I want more than a religion — I want You. In Jesus' name, amen."
        ),
        "Psalm 91:1-2" to (
            "'The secret place of the most High' isn't a physical location — it's a posture of dwelling close to God. When you make Him your refuge, you're not promised an easy life, but you ARE promised a safe one in the truest sense. The shadow of the Almighty is the best shelter there is." to
            "Lord, I make You my refuge today. Cover me in Your shadow. Whatever storms hit, let me dwell close enough to You to be unshaken. In Jesus' name, amen."
        ),
        "Psalm 119:105" to (
            "God's Word doesn't floodlight the whole journey — it's a lamp for your feet. Just enough light for the next step. That's frustrating if you want to see five years ahead, but it's exactly what you need to walk faithfully today. Read His Word; take the next step it shows you." to
            "Father, thank You for Your Word. When I want a full map and You give me a lamp, help me walk anyway. Light my path today, one step at a time. In Jesus' name, amen."
        ),
        "Isaiah 40:31" to (
            "Those who 'wait upon the Lord' don't just sit around — they renew their strength. Eagles don't flap their way to high altitude; they catch updrafts. When you wait on God in prayer, in His Word, in patient trust, He gives you altitude you can't generate on your own." to
            "Lord, I'm tired. I've been flapping my own wings, trying to lift myself. Today I wait on You. Renew my strength. Lift me on Your wind. In Jesus' name, amen."
        ),
        "Micah 6:8" to (
            "God isn't impressed with religious performance. What He wants is clear: do justly (be fair, stand for what's right), love mercy (be kind even when people don't deserve it), walk humbly (remember He's God and you're not). Three lifelong practices. Pick one to focus on today." to
            "Father, strip away my performative religion. Help me actually do justice today, love mercy when it's hard, and walk humbly with You. In Jesus' name, amen."
        ),
        "Zephaniah 3:17" to (
            "Picture this: the God of the universe SINGING over you with joy. Not tolerating you, not putting up with you — singing. Resting in His love for you. This is who God is. Receive His delight today instead of working to earn it." to
            "Father, it's hard to imagine You delighting in me. Help me to receive Your love as joyfully as You give it. Quiet me with Your love today. In Jesus' name, amen."
        ),
        "John 14:6" to (
            "Jesus didn't say He shows the way — He IS the way. He's not one option among many; He's the only path to the Father. That sounds narrow until you remember that God Himself opened it. Walk this Way today — not religion, not philosophy, but a person." to
            "Jesus, You are my way, my truth, and my life. Thank You for opening the path to the Father. I follow You today — not a system, but You. In Your name, amen."
        ),
        "John 15:5" to (
            "A branch doesn't strain to produce fruit — it just stays connected to the vine, and fruit happens. Same with you and Jesus. 'Apart from me you can do NOTHING' is humbling — even your best efforts produce nothing eternal without Him. Today, focus less on producing and more on abiding." to
            "Jesus, I want to stay connected to You today. Less striving, more abiding. Let Your life flow through me into whatever You're producing in this season. In Your name, amen."
        ),
        "Romans 5:8" to (
            "God didn't wait until you got your life together to love you — He demonstrated His love WHILE you were still in your sin. Christ died for the messy, undeserving version of you. This is grace at its purest. You're loved at your worst, not your best." to
            "Father, thank You for loving me as I am, not as I should be. Help me to love others the same way today — not because they've earned it, but because You loved me first. In Jesus' name, amen."
        ),
        "Romans 10:9" to (
            "Salvation isn't complicated — it's confession + belief. Confess Jesus as Lord (He's in charge), believe God raised Him from the dead (the resurrection is real). That's the gospel doorway. If you've never walked through it, today is the day. If you have, live like Jesus is actually Lord today." to
            "Jesus, You are my Lord and my Savior. I believe You died for my sins and rose again. Reign in my life today — not just on Sundays. In Your name, amen."
        ),
        "1 Corinthians 13:4-7" to (
            "Read this slowly and replace 'charity' with your own name. 'Maria suffereth long, is kind, envieth not, vaunteth not herself…' — does it ring true? This is what love looks like when Christ shapes you. Don't beat yourself up — let the Spirit grow this in you, one decision at a time." to
            "Father, I fall short of this kind of love. Grow it in me. Today, help me to be patient when I want to snap, kind when I want to retaliate, and humble when I want to be right. In Jesus' name, amen."
        ),
        "Galatians 5:22-23" to (
            "These nine qualities aren't a checklist YOU produce — they're FRUIT the Holy Spirit grows in you. Your job is to stay connected to the Spirit; the fruit forms naturally. Today, pick one (patience? self-control?) and pray for the Spirit to grow that specific fruit in you." to
            "Holy Spirit, fill me today. Grow Your fruit in me — especially ____, the one I need most this week. I can't manufacture it; You can. In Jesus' name, amen."
        ),
        "Ephesians 4:32" to (
            "Forgiveness flows downhill from the cross. God forgave you for Christ's sake — and now you extend that same forgiveness to others. Not because they deserve it, but because you've received what you didn't deserve. Today, who do you need to forgive? Maybe even yourself?" to
            "Father, thank You for forgiving me. Help me to forgive ____ as You forgave me — fully, freely. Soften my heart toward them. In Jesus' name, amen."
        ),
        "Philippians 4:6-7" to (
            "Anxiety has a specific antidote: prayer + thanksgiving. Not 'try not to worry' — exchange worry for prayer. And the result isn't that the situation always changes; it's that God's PEACE guards your heart and mind, even before the answer arrives. Today, swap one worry for one prayer." to
            "Father, I bring You what I've been worried about: ____. Thank You for ____ (something good even in this). Replace my anxiety with Your peace. In Jesus' name, amen."
        ),
        "Colossians 3:23" to (
            "Whatever you're doing today — schoolwork, chores, your part-time job, even cleaning your room — God says: do it for ME, not for people. That changes the motivation. The audience of One. Boring tasks become acts of worship when done for Him." to
            "Lord, I confess I work hard when I'm being watched and slack when I'm not. Today I'm doing my work for You. Help me to give it my full heart, not for applause but for Your honor. In Jesus' name, amen."
        ),
        "2 Timothy 1:7" to (
            "Fear didn't come from God. What He gives you is power (His Spirit), love (toward others), and a sound mind (clear thinking). When fear shows up, recognize it's not from God and reach for what He actually gave you. Today, refuse the spirit of fear in His name." to
            "Father, thank You that fear isn't from You. Today I receive power, love, and a sound mind from Your Spirit. Where I've been giving in to fear, give me Your boldness instead. In Jesus' name, amen."
        ),
        "Hebrews 12:1-2" to (
            "You're not running this race alone — a great cloud of witnesses has gone before you. Strip away every weight (the distractions, the sin you keep apologizing for but keep choosing). Keep your eyes on Jesus, the One who endured the cross to get to you. He's why you can keep running." to
            "Jesus, fix my eyes on You today. Help me lay aside what's slowing me down. When I want to quit, remind me of the joy set before me — being fully with You. In Your name, amen."
        ),
        "James 4:7" to (
            "Two parts to spiritual victory: SUBMIT to God, then RESIST the devil. People skip the first and wonder why the second isn't working. Submission isn't weakness; it's lining yourself up under God's authority so His power covers you. Submit first; then resist. The enemy will flee." to
            "Father, I submit myself fully to You today — my plans, my preferences, my reactions. In Your name I resist every lie and temptation. The enemy has no place here. In Jesus' name, amen."
        ),
        "1 John 1:9" to (
            "Confession isn't groveling — it's agreeing with God about what's true. When you confess sin, He's faithful (will keep His promise) and just (Jesus already paid) to forgive AND cleanse. Not just forgiven on paper — actually washed clean. Don't carry yesterday's sin into today." to
            "Father, I confess ____. Thank You for being faithful to forgive me and just to cleanse me. I receive Your forgiveness fully. Help me to walk free of yesterday today. In Jesus' name, amen."
        ),
        "1 John 4:19" to (
            "Your love for God isn't the starting point — His love for you is. Before you knew Him, before you wanted Him, He loved you. Every time you love Him back, you're responding to a love that came first. Today, let His love be the well you draw from instead of trying to manufacture love from your own tank." to
            "Father, thank You for loving me first. When my love feels weak, remind me Yours is strong. Fill me today so I can love others out of Your fullness, not my emptiness. In Jesus' name, amen."
        ),
        "Revelation 21:4" to (
            "There's coming a day when God Himself wipes the tears from your eyes. No more death. No more pain. No more grief. This isn't fantasy — it's the destination God's bringing His people to. Whatever you're crying about today is real, and so is this future. Hold both." to
            "Father, thank You that this isn't the end of the story. Hold me in the pain of today with hope for the joy that's coming. Wipe my tears, even now. In Jesus' name, amen."
        ),
        "Psalm 139:14" to (
            "You are FEARFULLY and WONDERFULLY made. Not a mistake, not random, not too much, not too little. Every detail of you — even the parts you wish were different — was knit together by God. When the world says you're not enough, Scripture says you were intentionally crafted." to
            "Father, thank You for making me on purpose. When I'm tempted to hate parts of myself, remind me they came from Your hand. Help me to walk today in the worth You gave me. In Jesus' name, amen."
        ),
        "Matthew 5:14-16" to (
            "You are the LIGHT — not just a candle to look at, but a flame to give light. Don't hide it under cultural pressure to fit in or fear of being misunderstood. Let people see your good works, and let the credit go to your Father in heaven. Today, where can you let your light shine?" to
            "Father, give me courage to shine today. Not for my own glory, but for Yours. Where I've been hiding my faith, help me to let it be seen. In Jesus' name, amen."
        ),
        "Matthew 28:19-20" to (
            "Jesus' final command was clear: make disciples. Not just converts — disciples. People who learn to follow Him as you do. The promise attached: 'I am with you always.' He doesn't send you out alone. Today, who's the one person God's calling you to point toward Jesus?" to
            "Jesus, give me courage to talk about You with the people You've put around me. Show me one person to invest in. Thank You for being with me always. In Your name, amen."
        ),
        "Mark 12:30-31" to (
            "Jesus boiled the whole law down to TWO commands: love God with everything, love your neighbor as yourself. Notice how 'neighbor' starts with how you love YOURSELF. You can't pour out what you haven't received. Receive God's love today; let it overflow to one neighbor." to
            "Father, I love You with all I have today — heart, soul, mind, strength. Help me to love the people You put in front of me as You love them. In Jesus' name, amen."
        ),
        "Luke 1:37" to (
            "The angel said this to Mary when the news he brought was IMPOSSIBLE. A virgin would conceive. God's promises don't bend to human logic — He doesn't need our help to keep them. Whatever 'impossible' you're facing right now, this verse is for you too." to
            "Father, with You nothing is impossible. The thing I've been calling impossible — I bring it to You. Show me what You can do that I can't. In Jesus' name, amen."
        ),
        "Acts 1:8" to (
            "The Holy Spirit isn't a 'mood boost' — He's POWER. Power to live for Jesus, power to be a witness even when it costs something. Notice the geography: Jerusalem (close), Judea, Samaria (uncomfortable), the ends of the earth (way out). God's mission has concentric circles, and you're inside one of them today." to
            "Holy Spirit, fill me with Your power today. Make me a faithful witness wherever You've placed me — at home, at school, in the places that scare me. In Jesus' name, amen."
        ),
        "Romans 1:16" to (
            "Paul wasn't ashamed of the gospel — and he wrote that to the church in ROME, the cultural center of the most powerful empire on earth. Different empire today, same gospel. The good news of Jesus is still God's POWER to save anyone who believes. Don't shrink it down today." to
            "Father, give me Paul's boldness. I don't want to be ashamed of the gospel — it's the power that saved me. Help me carry it confidently into today. In Jesus' name, amen."
        ),
        "Hebrews 13:5" to (
            "Contentment isn't pretending you have everything you want — it's trusting that you have everything you NEED in God. He's promised never to leave you. Money will come and go; God stays. Today, where are you tempted to think 'I'd be happy if I just had ___'? Bring it to Him." to
            "Father, thank You that You'll never leave me. Where I've been chasing things, help me to find my contentment in You. You're enough. In Jesus' name, amen."
        ),
        "Isaiah 53:5" to (
            "Look at what Jesus took ON Himself: your transgressions (the lines you've crossed), your iniquities (the brokenness in you). The chastisement for YOUR peace was on HIM. By His stripes — His wounds — you are healed. Healing started at the cross. Receive it today." to
            "Jesus, thank You for taking what I deserved so I could receive what You deserved. By Your wounds I am healed. Apply Your healing to my body, my mind, my soul today. In Your name, amen."
        ),

        "Luke 2:10-11" to (
            "'Good tidings of great joy' — not for a select few, but for ALL people. The shepherds heard it first: ordinary working-class men, not religious elites. The Savior didn't come for the put-together; He came for everyone. Today, receive the joy that Christmas actually announces." to
            "Father, thank You for sending Jesus for all people — including me. Let the joy of Christmas reach the places in me that have been heavy. The Savior is born; I rejoice. In Jesus' name, amen."
        ),
        "Isaiah 9:6" to (
            "Each name tells you who Jesus is: Wonderful Counsellor (He has wisdom for your real questions), Mighty God (no problem is too big), Everlasting Father (His love doesn't run out), Prince of Peace (He brings calm to chaos). Pick one name and lean into it today." to
            "Jesus, You are my Wonderful Counsellor, my Mighty God, my Everlasting Father, my Prince of Peace. Be all of those to me today. Especially ____, where I need You most. In Your name, amen."
        ),
        "Matthew 1:21" to (
            "The name JESUS means 'the Lord saves.' He wasn't named after a relative; He was named for His mission. He came specifically to save His people from their SINS — not just from circumstances. Christmas without that center is just decoration. Receive what He came to give." to
            "Jesus, thank You for coming for the express purpose of saving me from my sins — not just my problems. Forgive me, save me, transform me. In Your name, amen."
        ),
        "Micah 5:2" to (
            "God chose Bethlehem — small, overlooked, no obvious importance — to be where the Ruler of Israel was born. He delights in using small things. Whatever feels overlooked about your story today, God can write a Bethlehem-sized moment in it." to
            "Father, You delight in the overlooked. Where I feel small or insignificant, remind me that's exactly where You love to work. Use my story for something bigger than me. In Jesus' name, amen."
        ),
        "John 1:14" to (
            "God became FLESH. Not a hologram, not a metaphor — actual skin and bones. He moved into the neighborhood. Whatever you struggle with today — temptation, exhaustion, loneliness — Jesus has lived in human skin and gets it. He's full of grace and truth. Both. Together." to
            "Jesus, thank You for not staying distant. You became flesh so I could know You. Be near me in my actual humanity today — full of grace AND truth. In Your name, amen."
        ),
        "Luke 2:14" to (
            "The angels announced 'peace on earth' the night Jesus was born — not because all conflict ended, but because the source of true peace had arrived. God's good will toward humans was unveiled at the manger. Today, where you need peace, look to the One whose birth announced it." to
            "Father, thank You for sending peace in the form of a person. Where my heart is at war today, bring Your peace. Where there's conflict around me, make me a peacemaker. In Jesus' name, amen."
        ),
        "Galatians 4:4-5" to (
            "'When the fulness of the time was come' — God's timing was perfect. Not too early, not too late. He sent Jesus to redeem those under the law so we could be adopted as sons and daughters. You're not a servant of God; you're family. Walk like a daughter or son today." to
            "Father, thank You for adopting me into Your family through Jesus. Help me to live today not as a servant trying to earn approval, but as Your child, already loved. In Jesus' name, amen."
        ),
        "Matthew 2:10-11" to (
            "The wise men traveled hundreds of miles, found a toddler in an ordinary house, and worshipped. They gave Him their best treasures. Worship is the right response when you actually see who Jesus is. Today, what 'treasure' (time, attention, plans) can you bring Him?" to
            "Jesus, You're worthy of my best — not my leftovers. Today I bring You ____ as worship. Receive it. In Your name, amen."
        ),
        "Isaiah 7:14" to (
            "'Immanuel' means GOD WITH US. Not God watching from above, not God waiting in heaven — God with us, in the dirt, in the everyday. That's Christmas in one word. Wherever you are today, He's with you. Not aware OF you — WITH you." to
            "Immanuel, thank You that You are God WITH me. Not distant. With me in the boring moments, the hard ones, the small ones. Let me feel Your presence today. In Jesus' name, amen."
        ),
        "John 3:17" to (
            "Christmas wasn't God sending Jesus to condemn the world — it was God sending Jesus to SAVE it. If you've been carrying shame today, hear it again: He didn't come to condemn you. He came to save you. Receive salvation; release condemnation." to
            "Jesus, thank You for coming to save me, not to condemn me. I lay down the shame I've been carrying. Save me, fully and forever. In Your name, amen."
        ),

        "Mark 11:9-10" to (
            "Palm Sunday: people shouted 'Hosanna!' (which means 'save us, please!'). Days later many in the same crowd would shout 'Crucify Him!' Crowds shift fast. Your faith can't ride on a crowd's energy — it has to be anchored in who Jesus really is. Today, what do YOU say about Him?" to
            "Jesus, save me. Not the loud, popular version of faith — the real one that lasts. Anchor me in You today regardless of what the crowd around me says. In Your name, amen."
        ),
        "John 13:34-35" to (
            "The night before He died, Jesus gave one new commandment: love each other as I have loved you. THIS is what proves we're His. Not our doctrine, not our church attendance — our love. Where in your relationships today does Christ's love need to show up?" to
            "Jesus, help me love the way You loved me — sacrificially, patiently, faithfully. Show me one person to love well today. May they see You in how I treat them. In Your name, amen."
        ),
        "Psalm 22:1" to (
            "Jesus quoted this Psalm from the cross. Even Jesus felt forsaken. If He did, you can too — and you can take it to God like David did, in raw honesty. Don't pretty up your prayers today. God can handle your real questions." to
            "Father, sometimes I feel forsaken. Like You're distant when I need You most. I bring my real feelings to You today — not the polished ones. Meet me here. In Jesus' name, amen."
        ),
        "John 19:30" to (
            "'It is finished' — in Greek, ONE word: tetelestai. It meant 'paid in full,' stamped on receipts when a debt was settled. Your debt for sin? Paid in full at the cross. You can't add to a finished work. Stop trying to earn what Jesus already secured. Just receive." to
            "Jesus, thank You for finishing the work on the cross. I don't have to add to it — I just have to receive. Forgive me for striving when You've already paid. In Your name, amen."
        ),
        "Matthew 27:54" to (
            "Even a hardened Roman centurion watching Jesus die said 'Truly this was the Son of God.' The most unlikely person, in the most unlikely moment, recognized the truth. God can reveal Himself to anyone — and the cross is the clearest place to see Him. Look there today." to
            "Father, You revealed Jesus even to a Roman soldier at the cross. Reveal Yourself to the unlikely people in my life. Open eyes — including mine — today. In Jesus' name, amen."
        ),
        "Matthew 28:6" to (
            "'He is not here: for He is risen.' This is the line that changes everything. If Jesus stayed dead, Christianity is a sad memorial. But He rose — and that means death lost, sin lost, the enemy lost. You serve a risen Jesus, not a dead idea." to
            "Jesus, You are risen! Death couldn't hold You — and because of that, nothing can hold me. Help me live today in the power of Your resurrection. In Your name, amen."
        ),
        "Romans 6:4" to (
            "Baptism is a picture: you were buried with Christ and raised to a NEW life. The old version of you stayed in the grave. Today, walk in the newness — not the old patterns, not the old shame, not the old fears. You were raised for something new." to
            "Father, thank You that the old me was buried with Christ. Help me walk today in the new life You raised me to. Old patterns, stay buried. In Jesus' name, amen."
        ),
        "1 Corinthians 15:55-57" to (
            "Death used to have a sting. Now? Christ has taken it. The grave used to win. Now? Empty. Whatever loss you're carrying today, the resurrection means it doesn't get the last word. Through Jesus, you have victory — already secured, even when it doesn't feel like it." to
            "Father, thank You for the victory You give through Jesus. Death and the grave don't win in my story. Where I'm grieving today, hold me with the truth of the resurrection. In Jesus' name, amen."
        ),

        "Acts 2:1-4" to (
            "When the Holy Spirit came, He came POWERFULLY — wind, fire, courage, new languages. Pentecost wasn't a one-time event for the early church only; the same Spirit lives in every believer. He's not a trickle; He's a rushing wind. Today, ask for fresh filling." to
            "Holy Spirit, fill me afresh today. Blow through every place in me that's grown stale. Set me on fire for Jesus again. In His name, amen."
        ),
        "Acts 2:38" to (
            "Peter's first sermon called for two things: repent (turn around, change direction) and be baptized (publicly identify with Jesus). The gift attached: the Holy Spirit. If you've never publicly identified with Christ, today's a good day to talk to a leader about it." to
            "Father, I turn from my sin today and back to You. Thank You for forgiveness and for the gift of Your Holy Spirit. Make my life a public 'yes' to Jesus. In His name, amen."
        ),
        "John 14:26" to (
            "Jesus didn't leave you to figure faith out on your own — He sent the Holy Spirit as a Comforter and Teacher. The Spirit reminds you of what Jesus said when you're tempted to forget. Today, listen for those reminders. They're not random thoughts; they're Him." to
            "Holy Spirit, thank You for being my Comforter and Teacher. Bring Jesus' words back to my heart when I need them most. Teach me what I don't yet understand. In Jesus' name, amen."
        ),
        "Joel 2:28" to (
            "God promised to pour out His Spirit on ALL flesh — including young people, including women, including the unexpected. The Spirit didn't bypass youth then, and He doesn't now. Your age isn't a disqualifier; it's an invitation. What dream is the Spirit putting in you?" to
            "Father, pour out Your Spirit on me. I'm young, but Your Spirit isn't limited by age. Show me the visions and dreams You're inviting me into. In Jesus' name, amen."
        ),
        "Ephesians 1:13-14" to (
            "When you believed, you were SEALED with the Holy Spirit. A seal in the ancient world meant ownership and protection. The Spirit in you is God's stamp: 'This one is mine.' He's also a guarantee — proof that everything He's promised for eternity is coming." to
            "Father, thank You for sealing me with Your Spirit. I belong to You, fully and forever. Help me to walk today in the security of being Yours. In Jesus' name, amen."
        )
    )

    private val verses: List<CatalogueVerse> = listOf(
        CatalogueVerse("John 3:16", Season.GENERAL),
        CatalogueVerse("Jeremiah 29:11", Season.GENERAL),
        CatalogueVerse("Philippians 4:13", Season.GENERAL),
        CatalogueVerse("Romans 8:28", Season.GENERAL),
        CatalogueVerse("Psalm 23:1", Season.GENERAL),
        CatalogueVerse("Proverbs 3:5-6", Season.GENERAL),
        CatalogueVerse("Isaiah 41:10", Season.GENERAL),
        CatalogueVerse("Matthew 6:33", Season.GENERAL),
        CatalogueVerse("Joshua 1:9", Season.GENERAL),
        CatalogueVerse("Psalm 46:10", Season.GENERAL),
        CatalogueVerse("Romans 12:2", Season.GENERAL),
        CatalogueVerse("2 Corinthians 5:17", Season.GENERAL),
        CatalogueVerse("Galatians 2:20", Season.GENERAL),
        CatalogueVerse("Ephesians 2:8-9", Season.GENERAL),
        CatalogueVerse("Hebrews 11:1", Season.GENERAL),
        CatalogueVerse("James 1:2-3", Season.GENERAL),
        CatalogueVerse("1 Peter 5:7", Season.GENERAL),
        CatalogueVerse("Matthew 11:28-29", Season.GENERAL),
        CatalogueVerse("Lamentations 3:22-23", Season.GENERAL),
        CatalogueVerse("Psalm 27:1", Season.GENERAL),
        CatalogueVerse("Psalm 34:8", Season.GENERAL),
        CatalogueVerse("Psalm 91:1-2", Season.GENERAL),
        CatalogueVerse("Psalm 119:105", Season.GENERAL),
        CatalogueVerse("Isaiah 40:31", Season.GENERAL),
        CatalogueVerse("Micah 6:8", Season.GENERAL),
        CatalogueVerse("Zephaniah 3:17", Season.GENERAL),
        CatalogueVerse("John 14:6", Season.GENERAL),
        CatalogueVerse("John 15:5", Season.GENERAL),
        CatalogueVerse("Romans 5:8", Season.GENERAL),
        CatalogueVerse("Romans 10:9", Season.GENERAL),
        CatalogueVerse("1 Corinthians 13:4-7", Season.GENERAL),
        CatalogueVerse("Galatians 5:22-23", Season.GENERAL),
        CatalogueVerse("Ephesians 4:32", Season.GENERAL),
        CatalogueVerse("Philippians 4:6-7", Season.GENERAL),
        CatalogueVerse("Colossians 3:23", Season.GENERAL),
        CatalogueVerse("2 Timothy 1:7", Season.GENERAL),
        CatalogueVerse("Hebrews 12:1-2", Season.GENERAL),
        CatalogueVerse("James 4:7", Season.GENERAL),
        CatalogueVerse("1 John 1:9", Season.GENERAL),
        CatalogueVerse("1 John 4:19", Season.GENERAL),
        CatalogueVerse("Revelation 21:4", Season.GENERAL),
        CatalogueVerse("Psalm 139:14", Season.GENERAL),
        CatalogueVerse("Matthew 5:14-16", Season.GENERAL),
        CatalogueVerse("Matthew 28:19-20", Season.GENERAL),
        CatalogueVerse("Mark 12:30-31", Season.GENERAL),
        CatalogueVerse("Luke 1:37", Season.GENERAL),
        CatalogueVerse("Acts 1:8", Season.GENERAL),
        CatalogueVerse("Romans 1:16", Season.GENERAL),
        CatalogueVerse("Hebrews 13:5", Season.GENERAL),
        CatalogueVerse("Isaiah 53:5", Season.GENERAL),

        CatalogueVerse("Luke 2:10-11", Season.CHRISTMAS),
        CatalogueVerse("Isaiah 9:6", Season.CHRISTMAS),
        CatalogueVerse("Matthew 1:21", Season.CHRISTMAS),
        CatalogueVerse("Micah 5:2", Season.CHRISTMAS),
        CatalogueVerse("John 1:14", Season.CHRISTMAS),
        CatalogueVerse("Luke 2:14", Season.CHRISTMAS),
        CatalogueVerse("Galatians 4:4-5", Season.CHRISTMAS),
        CatalogueVerse("Matthew 2:10-11", Season.CHRISTMAS),
        CatalogueVerse("Isaiah 7:14", Season.CHRISTMAS),
        CatalogueVerse("John 3:17", Season.CHRISTMAS),

        CatalogueVerse("Mark 11:9-10", Season.HOLY_WEEK),
        CatalogueVerse("John 13:34-35", Season.HOLY_WEEK),
        CatalogueVerse("Isaiah 53:5", Season.HOLY_WEEK),
        CatalogueVerse("Psalm 22:1", Season.HOLY_WEEK),
        CatalogueVerse("John 19:30", Season.HOLY_WEEK),
        CatalogueVerse("Matthew 27:54", Season.HOLY_WEEK),
        CatalogueVerse("Matthew 28:6", Season.HOLY_WEEK),
        CatalogueVerse("Romans 6:4", Season.HOLY_WEEK),
        CatalogueVerse("1 Corinthians 15:55-57", Season.HOLY_WEEK),

        CatalogueVerse("Acts 2:1-4", Season.PENTECOST),
        CatalogueVerse("Acts 2:38", Season.PENTECOST),
        CatalogueVerse("John 14:26", Season.PENTECOST),
        CatalogueVerse("Joel 2:28", Season.PENTECOST),
        CatalogueVerse("Galatians 5:22-23", Season.PENTECOST),
        CatalogueVerse("Acts 1:8", Season.PENTECOST),
        CatalogueVerse("Ephesians 1:13-14", Season.PENTECOST)
    )

    init {
        val missing = verses.map { it.ref }.toSet() - kjvText.keys
        require(missing.isEmpty()) {
            "VerseCatalogue refs missing KJV text: $missing"
        }
        val missingDevo = verses.map { it.ref }.toSet() - reflectionsAndPrayers.keys
        require(missingDevo.isEmpty()) {
            "VerseCatalogue refs missing reflection/prayer: $missingDevo"
        }
    }

    private val byseason: Map<Season, List<String>> =
        verses.groupBy { it.season }.mapValues { (_, list) -> list.map { it.ref } }

    private val easterByYear: Map<Int, LocalDate> = mapOf(
        2025 to LocalDate.of(2025, 4, 20),
        2026 to LocalDate.of(2026, 4, 5),
        2027 to LocalDate.of(2027, 3, 28),
        2028 to LocalDate.of(2028, 4, 16),
        2029 to LocalDate.of(2029, 4, 1),
        2030 to LocalDate.of(2030, 4, 21),
        2031 to LocalDate.of(2031, 4, 13),
        2032 to LocalDate.of(2032, 3, 28),
        2033 to LocalDate.of(2033, 4, 17),
        2034 to LocalDate.of(2034, 4, 9),
        2035 to LocalDate.of(2035, 3, 25)
    )

    fun verseFor(date: LocalDate): String = pickRef(date)

    fun verseAndTextFor(date: LocalDate): Pair<String, String> {
        val ref = pickRef(date)
        return ref to kjvText.getValue(ref)
    }

    fun textForRef(ref: String): String? = kjvText[ref]

    fun devotionalContentFor(ref: String): Pair<String, String>? =
        reflectionsAndPrayers[ref]

    private fun pickRef(date: LocalDate): String {
        val season = seasonFor(date)
        if (season != Season.GENERAL) {
            val pool = byseason[season].orEmpty()
            if (pool.isNotEmpty()) {
                val idx = Math.floorMod(date.dayOfYear.toLong(), pool.size.toLong())
                return pool[idx.toInt()]
            }
        }
        val pool = byseason[Season.GENERAL].orEmpty()
        require(pool.isNotEmpty()) { "GENERAL verse pool must not be empty" }
        val yearSeed = (date.year.toLong() xor 0xC0FFEEL)
        val shuffled = pool.shuffled(Random(yearSeed))
        val idx = Math.floorMod(date.dayOfYear.toLong() - 1, shuffled.size.toLong())
        return shuffled[idx.toInt()]
    }

    fun seasonFor(date: LocalDate): Season {
        val isLateDec = date.monthValue == 12 && date.dayOfMonth >= 20
        val isJan1 = date.monthValue == 1 && date.dayOfMonth == 1
        if (isLateDec || isJan1) return Season.CHRISTMAS

        val easter = easterByYear[date.year] ?: return Season.GENERAL
        val palmSunday = easter.minusDays(7)
        if (!date.isBefore(palmSunday) && !date.isAfter(easter)) {
            return Season.HOLY_WEEK
        }
        val pentecost = easter.plusDays(49)
        val pentecostStart = pentecost.minusDays(6)
        if (!date.isBefore(pentecostStart) && !date.isAfter(pentecost)) {
            return Season.PENTECOST
        }
        return Season.GENERAL
    }
}
