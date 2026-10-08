"""Fictional Cedar Court fixtures. Kept out of application and migration code."""

FEMALE_NAMES = """
Aditi Deshpande; Anushka Rao; Riya Kapoor; Sneha Kulkarni; Pooja Verma; Neha Joshi;
Kavya Menon; Ishita Bose; Nandini Gupta; Shruti Patil; Tanvi Shah; Sakshi Jain;
Ritu Bhatia; Pallavi Naik; Anjali Sinha; Shreya Das; Kritika Suri; Divya Reddy;
Rachana Shetty; Bhavna Arora; Malini Krishnan; Suman Ghosh; Vidya Prasad; Geeta Trivedi;
Lakshmi Nair; Seema Wadhwa; Madhuri Jagtap; Amrita Sen; Priyanka Dubey; Rashmi Saxena;
Swati Agarwal; Arpita Mukherjee; Aparna Pillai; Deepti Chawla; Supriya More; Sonia Grover;
Shalini Mishra; Farah Qureshi; Nisha Bansal; Saira Mirza; Renuka Hegde; Radhika Borkar;
Urmila Sawant; Asha Thomas; Gayatri Inamdar; Hema Rangan; Maya Dutta; Isha Bedi;
Rekha Ahuja; Lavanya Rao; Suchitra Kale; Aarti Dhawan; Komal Soni; Madhavi Acharya;
Roshni Fernandes; Savita Khanna; Leena Vora; Jasleen Kaur; Sheetal Kulkarni; Anupama Pandey
"""
MALE_NAMES = """
Aarav Kulkarni; Arjun Rao; Aditya Desai; Nikhil Sharma; Sameer Joshi; Saurabh Gupta;
Vikram Nair; Manish Mehta; Vivek Menon; Kunal Shah; Rahul Sinha; Rakesh Verma;
Amit Bansal; Sandeep Bhat; Aniket Patil; Pranav Kulkarni; Siddharth Reddy; Harsh Jain;
Varun Kapoor; Mohit Chawla; Abhishek Dubey; Ashish Saxena; Sachin More; Ajit Naik;
Deepak Trivedi; Gaurav Arora; Ritesh Shetty; Satish Prasad; Alok Mishra; Pradeep Ghosh;
Sudhir Borkar; Suresh Inamdar; Rajiv Khanna; Vinay Agarwal; Atul Soni; Yogesh Deshpande;
Abhay Dutta; Gautam Sen; Akash Vora; Sumit Bedi; Hemant Sawant; Dinesh Pillai;
Balaji Iyer; Anand Krishnan; Sharad Kale; Chirag Patel; Zubin Fernandes; Arvind Hegde;
Navin Pandey; Shirish Jagtap; Ranjit Ahuja; Imtiaz Khan; Salman Mirza; Faiz Siddiqui;
Harpreet Singh; Rohan Sethi; Nishant Kaul; Tejas Gokhale; Shantanu Biswas; Milind Kulkarni
"""

BIOS = [
    "Usually out for a walk around seven. Say hello if you see me near the garden.",
    "Working from home most days. Still trying to keep the balcony plants alive.",
    "Weekend cyclist. Happy to share a route if you're getting started.",
    "Moved here with my family last year. The courtyard is our favourite bit.",
    "Always looking for a good book recommendation.",
    "Early riser, terrible at badminton, trying anyway.",
    "I bake when there is an excuse. Birthdays count.",
    "Retired recently. You will usually find me with the evening walking group.",
    "Happy to help with the book shelf in the clubhouse.",
    "Office days are busy, but usually around on weekends.",
    "Learning to garden one pot at a time.",
    "Often at the badminton court on Sunday mornings.",
    "The person with far too many books and not enough shelves.",
    "Here with my partner and our very curious toddler.",
    "I work shifts, so I may take a while to reply.",
    "Happy to collect a parcel for a neighbour when I'm home.",
    "Love a quiet morning, filter coffee and the newspaper.",
    "Trying to get the children interested in growing their own vegetables.",
    "New to the evening yoga group. Enjoying it so far.",
    "I can lend a ladder or basic tools if you need them.",
    "Dog walks happen twice a day, whatever the weather.",
    "Usually at work during the day. Best reached in the evenings.",
    "We moved in for the shorter commute and stayed for the neighbours.",
    "Helping with the next courtyard clean-up.",
    "A little cricket on the weekend makes the week better.",
    "There is nearly always extra tea at our place.",
    "Keeping an eye out for a regular carpool to the metro.",
    "Music, long walks and an unreasonable collection of houseplants.",
    "Happy to exchange cuttings once the monsoon settles down.",
    "We have lived in this neighbourhood for several years.",
    "Usually around after six. Please knock if a parcel is left at our door.",
    "I help organise the occasional children's craft afternoon.",
    "Badminton on Sundays, errands on Saturdays.",
    "Recently got back into reading. The book exchange has been useful.",
    "Trying to make more time for neighbours and less time for my phone.",
    "We have a spare folding table if anyone needs one for a gathering.",
    "I am away a few days each month for work.",
    "Enjoying the quiet after years of living on a main road.",
    "Mostly here for the garden, the walks and a good chat.",
    "Happy to share local repair contacts that have worked for us.",
]

TECHNICIANS = [
    ("Suresh Pawar", "mason", "Small plaster repairs, tile work and balcony patching. Available on weekday mornings."),
    ("Vinod Yadav", "mason", "Tile replacement, grouting and masonry repairs. Please send a photo of the area before a visit."),
    ("Mahesh Jadhav", "roofer", "Terrace waterproofing and rainwater outlet repairs. Site visits by appointment."),
    ("Imran Sheikh", "roofer", "Waterproofing around terraces and exposed balconies. I check the source of the leak before quoting."),
    ("Ajay Salunke", "painter", "Interior paint, touch-ups and damp-patch preparation. Happy to work around occupied rooms."),
    ("Sanjay Das", "painter", "Repainting and smaller wall repairs. Materials and labour are quoted separately."),
    ("Nilesh Patil", "plumber", "Tap repairs, concealed leaks and drainage. Usually in the area before lunch."),
    ("Farhan Shaikh", "electrician", "Domestic wiring, fans and switchboards. Please mention the fault when booking."),
    ("Manoj Vishwakarma", "carpenter", "Door alignment, cabinet hinges and small furniture repairs. Most fittings can be done in one visit."),
    ("Adil Ansari", "hvac", "AC servicing, drain leaks and cooling faults. Evenings available with advance notice."),
]

# author unit, age in days, title, body, tags, replies: (author unit, text)
POSTS = [
    ("A-201", 0, "Spare seat to the metro tomorrow morning",
     "Leaving from the main gate at 8:10 tomorrow. I have one seat free if anyone is going towards the metro. Please be downstairs a couple of minutes early; I can't wait past 8:15.",
     ["Community"], [("B-103", "That works for me. Which gate do you normally leave from?"), ("A-201", "The main one by the security desk."), ("B-103", "Thanks, I'll be there at 8:05.")]),
    ("C-405", 0, "Small change to this week's wet-waste collection",
     "Spoke to the collection team this morning. They will come around 8 instead of 7:30 for the next three days while the other vehicle is being repaired. Please keep the buckets inside until they ring.",
     ["Building Updates"], [("A-502", "Thanks for checking. They missed our floor yesterday."), ("B-302", "They came at 8:05 in B today."), ("C-405", "Good to know. I'll let the first-floor neighbours know too.")]),
    ("B-104", 1, "Has anyone seen a grey lunch bag?",
     "My son left a small grey lunch bag near the benches yesterday evening. There is a steel box inside and a red bottle clipped to the handle. If someone picked it up, please let me know.",
     ["Lost & Found"], [("C-301", "There was one at the security desk this morning."), ("B-104", "That was it. Picked it up, thank you!")]),
    ("A-406", 1, "A few more people for Saturday badminton",
     "We have five people for the court at 7 on Saturday. Two or three more would make doubles easier. Nothing competitive, and beginners are very welcome. Bring your own racket if you have one.",
     ["Community", "Events"], [("C-501", "Count me in. Haven't played in years though."), ("B-205", "I have a spare racket I can bring."), ("A-406", "Perfect. Meet by the court at 6:55."), ("A-103", "I can join for the first hour.")]),
    ("C-204", 1, "Anyone using a water-saving shower head?",
     "Thinking of replacing the one in our bathroom. Looking for something that doesn't feel like standing under a dripping tap. If you have used one for a while, would you recommend it?",
     ["Recommendations"], [("A-304", "Ours is fine, but check the water pressure first. It struggled on the old line."), ("C-106", "We replaced the hose at the same time. That made more difference than the head."), ("C-204", "Useful, thanks. I'll check that before ordering.")]),
    ("B-603", 2, "Visitor parking near the B-block entrance",
     "Could we please leave the two marked visitor spaces free? A cab had to stop across the ramp yesterday because both were occupied all afternoon. I don't mind sharing my bay for a couple of hours if someone asks first.",
     ["Parking"], [("B-202", "Agree about the ramp. It gets difficult with a stroller too."), ("A-602", "One of those cars was ours while our bay was being cleaned. Sorry about that, moved it now."), ("B-603", "Thanks. A note at security would help next time.")]),
    ("C-103", 3, "Compost from the garden team is ready",
     "The garden team has a few small bags of compost for residents. They are keeping them near the shed, not beside the main bins. Take one bag for now so more flats get some. I tried it on the curry-leaf plant last month and it seems happy.",
     ["Gardening"], [("A-106", "Is it dry enough to use straight away?"), ("C-103", "Yes, just mix a little into the top layer of soil."), ("B-305", "Picked up a bag this morning. Thank you.")]),
    ("A-503", 4, "Board games in the clubhouse on Sunday",
     "A few of us are bringing games to the clubhouse from 4 to 6 on Sunday. We have Carrom, Uno and a couple of longer games. Drop in for one round if that's all you have time for. Children are welcome with a parent.",
     ["Community", "Events"], [("C-206", "We can bring Scrabble."), ("B-501", "I'll bring a carrom board. The clubhouse one is missing two pieces."), ("A-503", "Thanks! I'll bring chalk and a couple of cloths for the tables.")]),
    ("B-401", 5, "A-block side gate latch has been repaired",
     "The latch that kept catching on the side gate was adjusted this afternoon. It closes properly now, so please don't wedge it open with the stone. The security team has kept the stone aside.",
     ["Building Updates", "Maintenance"], [("A-102", "Tried it just now, much better."), ("A-305", "Thank you for following up on this.")]),
    ("C-602", 6, "Looking for a local tailor for small alterations",
     "Nothing urgent, just a couple of trousers that need shortening. Prefer somewhere I can walk to rather than sending them away for a week. Any recommendations from someone who has used the shop recently?",
     ["Recommendations"], [("B-106", "The shop next to the pharmacy did ours in two days."), ("C-602", "Is that the one in the lane behind the bus stop?"), ("B-106", "Yes, that one. Go before seven; they close early sometimes.")]),
    ("A-104", 8, "Please rinse the milk packets before putting them in dry waste",
     "The sorting team asked me to pass this on. A quick rinse makes their job much easier and keeps the collection corner from smelling. They also asked us not to tie wet waste inside plastic grocery bags.",
     ["Building Updates"], [("C-204", "Thanks for posting this. Does the same apply to curd tubs?"), ("A-104", "Yes, empty and rinse those as well."), ("B-304", "We keep a small bowl in the sink for this now. Easier than doing each packet separately.")]),
    ("B-502", 9, "Evening walk, a slightly later start?",
     "A few of us can't get downstairs by 6:30 on office days. Would anyone prefer a second group around 7:15? Two laps and a chat, no need to keep up with the fast walkers.",
     ["Community"], [("A-601", "7:15 would work much better for me."), ("C-402", "Same here. Shall we try Tuesday?"), ("B-502", "Tuesday it is. Near the neem tree."), ("C-105", "I'll join if I'm back in time.")]),
    ("C-306", 11, "A box of children's books to pass on",
     "We have outgrown about twenty books for ages five to eight. Mostly picture books, with a few early readers. They are used but in good condition. Happy to split the box if more than one family would like them.",
     ["Community"], [("A-205", "We would love a few early readers if they're still available."), ("B-102", "Could I take the picture books for my niece?"), ("C-306", "Yes to both. I'll keep two bags ready at home.")]),
    ("A-306", 12, "Rainwater drain outside the C-block lobby",
     "The drain beside the lobby is clearing much faster since it was cleaned yesterday. There are still loose leaves around the grate. Please avoid sweeping them back into the opening; the garden team will collect them separately.",
     ["Maintenance"], [("C-102", "It used to collect a puddle all the way to the door. Glad this is sorted."), ("C-504", "Walked through during the rain this evening and it was clear.")]),
    ("B-204", 14, "Pet owners: which morning slot works for a short meet-up?",
     "Our dog is a bit nervous around new dogs, and I'd like to introduce him slowly to the ones he sees every day. Would two or three owners be up for a short walk together, with everyone on leads? The quieter path behind C might work.",
     ["Pets", "Community"], [("C-203", "We can do Sunday at 8. Ours is quite calm."), ("A-404", "Please keep it off the children's play area. My daughter is still scared of dogs."), ("B-204", "Of course. We will stick to the back path.")]),
    ("C-401", 16, "Umbrella stand outside the clubhouse",
     "We put a plastic tray under the umbrella stand this evening. Please leave wet umbrellas there instead of on the chairs. The cleaner had to dry three chairs before the class could start.",
     ["Building Updates"], [("B-303", "Good idea. The floor was slippery last week."), ("A-504", "I'll bring an extra tray tomorrow if one is needed.")]),
    ("A-206", 18, "A reliable cycle repair person who comes to the gate?",
     "Two flat tyres and a brake cable that needs replacing. Has anyone used a repair person who can come here? I can take the cycle out if needed, but would rather get both children's cycles checked at the same time.",
     ["Recommendations"], [("B-405", "I use the shop near the petrol pump. They collect on weekday mornings."), ("C-202", "We used them too. Ask for a quote before they replace anything."), ("A-206", "Got them booked for Friday, thank you.")]),
    ("B-601", 20, "We found the missing carrom pieces",
     "They were in the drawer under the old noticeboard. The clubhouse board has a complete set again. Please count the pieces before putting it away; one of the children helped us search for nearly half an hour.",
     ["Community"], [("A-503", "Excellent timing for the next games afternoon."), ("C-406", "The drawer has claimed a few table-tennis balls as well!")]),
    ("C-101", 22, "Lift service on Thursday, 11 to 1",
     "The service engineer is coming for the scheduled check of the C-block lift on Thursday between 11 and 1. Please plan large deliveries around that slot. The office said they will put a notice up if the timing changes.",
     ["Building Updates", "Maintenance"], [("C-605", "Thanks, moving our grocery delivery to the afternoon."), ("C-308", "Please ask them to check the door sensor on the third floor too."), ("C-101", "Passed that on. They have added it to the list.")]),
    ("A-403", 24, "Seedlings looking for sunny balconies",
     "I sowed more basil and marigold than I have space for. There are six small pots ready to go. They need a few hours of sun and someone who remembers to water them. Bring a tray if you can; the pots are a bit wobbly.",
     ["Gardening"], [("B-506", "Could I have one basil pot?"), ("C-305", "I'll take two marigolds if there are any left."), ("A-403", "Kept them aside for both of you. After six works best.")]),
    ("B-206", 26, "The new corridor lights are a little bright",
     "The replacement light outside our door shines straight into the bedroom when the curtains are open. Has anyone else noticed this? Wondering whether it can be angled down slightly rather than changing the bulb again.",
     ["Maintenance"], [("B-201", "Same at our end. An angled cover might help."), ("C-506", "We had this last year; the electrician fitted a small diffuser."), ("B-206", "I'll ask about a diffuser. Thanks, that sounds simpler.")]),
    ("C-304", 28, "Carpool to the station on Friday evening",
     "Heading to the station at about 5:30 on Friday with one small suitcase. Happy to share a cab if someone is travelling around the same time. I need to be there by 6:30, so can't leave much later.",
     ["Community"], [("A-101", "My train is later, but an early arrival is fine. I can share."), ("C-304", "Great, I'll book a cab for 5:25.")]),
    ("A-602", 31, "Terrace drying racks after the rain",
     "Some of the shared racks are still dripping after yesterday's rain. We wiped down the ones on the east side. Please leave the central walkway clear when you set them out; the maintenance team needs access to the water tanks.",
     ["Building Updates"], [("C-601", "Thanks. We will shift ours away from the tank steps."), ("B-604", "The west-side rack has a loose leg. I've left it folded against the wall.")]),
    ("B-301", 34, "Would anyone like to help with the courtyard clean-up?",
     "Saturday at 8, for about an hour. The garden team is bringing bags and gloves. We are just collecting litter and clearing the benches, not pruning anything. A few extra pairs of hands would help.",
     ["Community", "Events"], [("A-302", "Two of us can come."), ("C-104", "I'll bring a flask of tea."), ("B-301", "Thanks everyone. We finished before nine and filled four bags."), ("A-302", "Nice to meet the C-block neighbours properly.")]),
    ("C-503", 37, "A note about courier calls",
     "A courier left three parcels at our door because our flat number was easier to find. Please put the block letter in the delivery address. I have handed all three to security, but it would save everyone a trip if the addresses were clearer.",
     ["Building Updates"], [("A-505", "One was mine. Sorry, the saved address had dropped the A."), ("B-203", "The courier app has a separate building field that seems to get hidden. I put the block in the first line now.")]),
    ("A-501", 40, "Simple recipes for all these tomatoes?",
     "My brother brought us far more tomatoes than we can use this week. I am making sauce, but would love another idea that freezes well. Also happy to give a small bag to a neighbour if you can collect it today.",
     ["Community"], [("C-303", "Roast them with garlic first. The sauce keeps much better."), ("B-404", "Tomato chutney freezes well in small portions."), ("A-501", "Tried the roasted version. The kitchen smells excellent.")]),
    ("B-105", 43, "Gate remote found near the mailboxes",
     "Black remote with a small green key ring. It was on the low wall beside the B-block mailboxes. Left it with the afternoon security guard rather than taking it upstairs.",
     ["Lost & Found"], [("B-402", "That's mine, thank you. Picked it up just now.")]),
    ("C-201", 46, "First aid kit in the clubhouse",
     "The cupboard beside the noticeboard now has a labelled first aid box. The office keeps the key if the clubhouse is closed. Please tell them if you use the last of something so it can be replaced.",
     ["Building Updates", "Safety"], [("A-303", "Good to know where it is. Is there a list on the cupboard?"), ("C-201", "Yes, the office added a small sign this afternoon."), ("B-101", "Thank you for getting this organised.")]),
    ("A-202", 49, "A small table available for anyone moving in",
     "We have a sturdy wooden side table that no longer fits in the room. It has a few marks on the top but nothing loose or broken. Free if you can carry it downstairs; it should fit in the lift.",
     ["Community"], [("C-502", "Could we see it this evening? We need one for the study."), ("A-202", "Sure, after seven is fine."), ("C-502", "Collected it. Fits perfectly, thanks again.")]),
    ("B-306", 53, "Water on the basement ramp this morning",
     "There was a slippery patch near the turn at the bottom of the ramp around 7. The cleaner has put a cone there and the office is checking where it came from. Please take the turn slowly until it is dry.",
     ["Maintenance", "Safety"], [("C-604", "Saw the cone when I left. Thanks for flagging it."), ("B-306", "Update: it was the washing hose, not a pipe leak. Dry now.")]),
    ("C-404", 57, "Could we keep the basketball a little earlier on Sundays?",
     "The bouncing near the bedroom side of C carries upstairs before 7. I don't want to stop anyone playing, but could we start at 8 on Sundays? Happy to discuss a different spot if that doesn't work for the regular group.",
     ["Community"], [("A-605", "Fair point. We'll start at eight this week."), ("B-403", "Eight also works better for the children here."), ("C-404", "Thank you, appreciate it.")]),
    ("A-105", 61, "Balcony mesh installer recommendation",
     "We had the balcony mesh refitted after the corner came loose in the wind. The installer reused the frame and only changed the damaged section, which saved a fair bit. Happy to share the contact if someone needs a similar small repair.",
     ["Recommendations"], [("B-505", "Please send it when you get a chance. Ours has the same problem."), ("C-205", "Would they do a kitchen window as well?"), ("A-105", "Yes, they measured our kitchen window too. I'll pass the number on.")]),
    ("B-602", 66, "Tea after the residents' meeting",
     "A few of us stayed back after the meeting and realised we only ever talk when something needs fixing. Would anyone be interested in an occasional informal tea in the courtyard? No agenda, and everyone brings their own cup.",
     ["Community", "Events"], [("C-403", "Much better than another group chat. I'm in."), ("A-301", "Saturday evenings would work for us."), ("B-602", "Let's try the first Saturday next month.")]),
    ("C-606", 72, "The terrace door should close behind you now",
     "The closer was adjusted today and the door is no longer slamming. It still needs a gentle push for the latch to catch. Please check it before going downstairs, especially if the wind picks up.",
     ["Maintenance"], [("A-604", "Tried it this evening. Much quieter."), ("C-603", "Thanks for following that up.")]),
    ("A-401", 79, "Looking for someone to water two pots next week",
     "Away from Tuesday to Friday. It is just a money plant and a jasmine pot, both on the balcony. I can leave them outside the door if that is easier, and will happily return the favour when you travel.",
     ["Gardening", "Community"], [("A-402", "We are next door, happy to do it."), ("A-401", "Thank you! I'll leave a small watering can with them.")]),
    ("B-503", 86, "Old newspapers for the school collection",
     "The children are collecting clean newspapers for a school recycling drive. If you have a small stack to spare, we can pick it up on Saturday morning. Please keep magazines separate; they have asked us to bring only newspaper this time.",
     ["Community"], [("C-505", "We have about a month's worth."), ("A-203", "A smaller pile here, but you are welcome to it."), ("B-503", "Collected everything this morning. The children were delighted with the response.")]),
]

# Trade-specific maintenance history and comments, selected independently.
REPAIRS = {
    "plumber": [
        ("Drip under the kitchen sink", "A slow drip collects where the waste pipe meets the sink. There is a bowl underneath for now."),
        ("Bathroom flush keeps running", "The cistern does not stop filling unless I lift the float by hand."),
        ("Kitchen mixer is stiff", "The tap handle has become difficult to turn and sometimes drips after closing."),
    ],
    "electrician": [
        ("Ceiling fan regulator not working", "The bedroom fan runs at full speed on every setting. The wall switch still works."),
        ("Kitchen light flickers", "The tube light takes several attempts to stay on, even after replacing the tube."),
        ("Loose switchboard in the study", "The switchboard has come away from the wall slightly. We have stopped using that socket."),
    ],
    "carpenter": [
        ("Wardrobe door catches at the bottom", "The right door rubs against the base and needs lifting slightly to close."),
        ("Kitchen cabinet hinge has loosened", "One hinge on the cabinet beside the sink no longer holds firmly."),
        ("Bedroom door latch does not line up", "The door closes but the latch misses the strike plate unless it is pushed upward."),
    ],
    "hvac": [
        ("AC drain pipe dripping indoors", "Water starts dripping from the indoor unit after about half an hour. It has been switched off."),
        ("Bedroom AC needs servicing", "Cooling is weaker than usual and the filter has been cleaned. Please check the coils and drain."),
        ("Outdoor AC unit vibrates", "There is a rattling sound from the outdoor bracket when the compressor starts."),
    ],
    "mason": [
        ("Loose tile near the kitchen doorway", "One tile rocks slightly when stepped on. The surrounding tiles seem secure."),
        ("Bathroom grout needs repair", "Several joints beside the shower have gaps and stay damp long after use."),
        ("Small plaster patch above the window", "A patch of plaster has flaked off above the window. The surface is dry now."),
    ],
    "roofer": [
        ("Damp patch after the last spell of rain", "A patch on the outer balcony ceiling darkens after heavy rain. Please inspect the exposed joint above it."),
        ("Balcony rainwater outlet needs checking", "Water collects near the outlet instead of draining away. The visible leaves have been cleared."),
        ("Waterproofing joint has opened", "The seal along the exposed terrace edge has a visible gap after the recent rain."),
    ],
    "painter": [
        ("Touch-up around the repaired kitchen pipe", "The pipe repair is finished and dry. The small wall patch needs primer and matching paint."),
        ("Bedroom wall needs a small repaint", "There are scuff marks behind the old cupboard. Only that wall needs attention."),
        ("Paint peeling beside the balcony frame", "The surface is dry, but the previous paint has lifted at the edge of the frame."),
    ],
}
REVIEWS = {
    "plumber": ["Found the leaking joint quickly and left the cupboard dry.", "Tap is working properly again. Came at the agreed time.", "Needed a second visit for the correct washer, but kept me informed.", "Explained what needed replacing before starting. No more drip.", "Cleaned up after the repair, which I appreciated.", "The repair is fine. Arrival was about an hour later than planned.", "Checked the nearby connection as well. It has stayed dry since."],
    "electrician": ["The fan is quiet again and all speeds work.", "Checked the wiring before changing the part. Sensible advice.", "Quick repair and no mess left behind.", "The light is finally staying on. Thank you.", "Had to come back with another fitting, but finished it the next morning.", "Good work. It was a little difficult to settle on a time.", "Tightened the board and explained why the old fitting had loosened."],
    "carpenter": ["Door closes properly now. A small adjustment made a big difference.", "Replaced the hinge and checked the other doors too.", "Brought the right fittings and finished within the hour.", "Neat work, though the first appointment had to be moved.", "No more scraping on the floor. Happy with the repair.", "Matched the handle closely enough that you can't really tell.", "Took care not to mark the cupboard. Would call again."],
    "hvac": ["Cooling has improved and the drain is clear.", "Showed me how to clean the filter between services.", "The vibration has stopped. Came with the correct mounting pads.", "Needed a follow-up visit for the drain, but it is sorted now.", "Arrived on time and covered the furniture before starting.", "Good service. The visit took longer than the estimate.", "Checked the outdoor unit as well instead of only cleaning the filter."],
    "mason": ["The repaired tile sits level with the rest.", "Small job, but finished carefully and cleaned up afterwards.", "Grout has held up well so far.", "Kept the dust contained and matched the finish reasonably well.", "Work is good. Finding a suitable slot took a few calls.", "Returned to check the patch after it had dried.", "Explained the drying time clearly so we could plan around it."],
    "roofer": ["We have had two heavy showers since the repair and the patch stayed dry.", "Cleared the outlet and checked the slope rather than just adding sealant.", "Explained which joint was letting water in. Neat finish.", "Had to wait for dry weather, but kept in touch about the visit.", "The repair took two visits. No further seepage so far.", "Careful inspection before quoting. That was reassuring.", "Water is draining properly again after the rain."],
    "painter": ["The touch-up blends in much better than I expected.", "Covered the furniture and cleaned the floor before leaving.", "Helped find a close colour match without repainting the whole room.", "Good finish. Needed a little more drying time than planned.", "Did the preparation properly instead of painting straight over the patch.", "The edges around the frame are tidy. Happy with it.", "Rescheduled once, but the actual work was careful and clean."],
}
