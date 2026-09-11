-- supabase/sql/exam_question_bank_class12_arts_set1.sql
--
-- First curated question-bank batch for 'Class 12 Arts' (see exam_patterns_class12_arts.sql for
-- the exam's structure). Same exam_question_bank table/schema as the other sets -- no migration
-- needed.
--
-- Every row here is format='mcq' with a single isCorrect:true option, matching this exam's
-- MCQ-only, no-negative-marking pattern. 8 chapters (2 per subject x 4 subjects: History,
-- Political Science, Geography, English) each get 4 questions -- one easy, one medium, two hard --
-- reflecting the final-board-year hard share in exam_patterns_class12_arts.sql. Pitched one notch
-- above Class 11 Arts (see exam_question_bank_class11_arts_set1.sql).
--
-- Run this once in the Supabase SQL Editor, after exam_patterns_class12_arts.sql.

insert into exam_question_bank (exam_name, subject, chapter, difficulty, format, text, options, numerical_answer, explanation) values

-- History: Mauryan-Gupta Empires and Mughal Court Culture
('Class 12 Arts', 'History', 'Mauryan-Gupta Empires and Mughal Court Culture', 'easy', 'mcq',
 'Which Mauryan emperor embraced Buddhism after the Kalinga War?',
 '[{"text":"Chandragupta Maurya","isCorrect":false},{"text":"Ashoka","isCorrect":true},{"text":"Bindusara","isCorrect":false},{"text":"Samudragupta","isCorrect":false}]'::jsonb,
 null, 'Emperor Ashoka embraced Buddhism after witnessing the bloodshed of the Kalinga War.'),
('Class 12 Arts', 'History', 'Mauryan-Gupta Empires and Mughal Court Culture', 'medium', 'mcq',
 'The Gupta period is often referred to as a "Golden Age" of India mainly due to advances in:',
 '[{"text":"Art, science, literature, and mathematics","isCorrect":true},{"text":"Military conquest alone","isCorrect":false},{"text":"Foreign trade alone","isCorrect":false},{"text":"Religious conflict","isCorrect":false}]'::jsonb,
 null, 'The Gupta period saw remarkable achievements in art, science, literature, and mathematics, earning it the title "Golden Age".'),
('Class 12 Arts', 'History', 'Mauryan-Gupta Empires and Mughal Court Culture', 'hard', 'mcq',
 'The Mauryan administrative text "Arthashastra", attributed to Chanakya (Kautilya), primarily deals with:',
 '[{"text":"Poetry and drama","isCorrect":false},{"text":"Statecraft, economic policy, and military strategy","isCorrect":true},{"text":"Religious rituals only","isCorrect":false},{"text":"Astronomy only","isCorrect":false}]'::jsonb,
 null, 'The Arthashastra is a treatise on statecraft, economic policy, and military strategy attributed to Chanakya.'),
('Class 12 Arts', 'History', 'Mauryan-Gupta Empires and Mughal Court Culture', 'hard', 'mcq',
 'The Mughal emperor known for his policy of religious tolerance and the concept of "Sulh-i-kul" (peace with all) was:',
 '[{"text":"Babur","isCorrect":false},{"text":"Akbar","isCorrect":true},{"text":"Aurangzeb","isCorrect":false},{"text":"Jahangir","isCorrect":false}]'::jsonb,
 null, 'Akbar promoted religious tolerance through his policy of "Sulh-i-kul", meaning peace with all.'),

-- History: Colonialism and Indian Nationalism (Advanced)
('Class 12 Arts', 'History', 'Colonialism and Indian Nationalism (Advanced)', 'easy', 'mcq',
 'The Indian National Congress was founded in which year?',
 '[{"text":"1885","isCorrect":true},{"text":"1905","isCorrect":false},{"text":"1857","isCorrect":false},{"text":"1920","isCorrect":false}]'::jsonb,
 null, 'The Indian National Congress was founded in 1885.'),
('Class 12 Arts', 'History', 'Colonialism and Indian Nationalism (Advanced)', 'medium', 'mcq',
 'The partition of Bengal in 1905, which fueled Indian nationalism, was reversed in which year?',
 '[{"text":"1911","isCorrect":true},{"text":"1919","isCorrect":false},{"text":"1909","isCorrect":false},{"text":"1915","isCorrect":false}]'::jsonb,
 null, 'The partition of Bengal was annulled in 1911 following widespread protest.'),
('Class 12 Arts', 'History', 'Colonialism and Indian Nationalism (Advanced)', 'hard', 'mcq',
 'The Cripps Mission of 1942 was sent to India by the British mainly to:',
 '[{"text":"Grant immediate full independence","isCorrect":false},{"text":"Secure Indian cooperation in World War II by offering future dominion status","isCorrect":true},{"text":"Impose direct martial law","isCorrect":false},{"text":"Abolish the Indian National Congress","isCorrect":false}]'::jsonb,
 null, 'The Cripps Mission aimed to secure Indian support for the British war effort by offering the promise of dominion status after the war.'),
('Class 12 Arts', 'History', 'Colonialism and Indian Nationalism (Advanced)', 'hard', 'mcq',
 'The Government of India Act of 1935 is significant because it:',
 '[{"text":"Granted full independence to India","isCorrect":false},{"text":"Provided for provincial autonomy and an all-India federation","isCorrect":true},{"text":"Abolished all provincial legislatures","isCorrect":false},{"text":"Ended British rule in India immediately","isCorrect":false}]'::jsonb,
 null, 'The Government of India Act 1935 introduced provincial autonomy and proposed an all-India federation, serving as a basis for India''s later constitution.'),

-- Political Science: The Cold War Era and Contemporary World Politics
('Class 12 Arts', 'Political Science', 'The Cold War Era and Contemporary World Politics', 'easy', 'mcq',
 'The Cold War was primarily a rivalry between which two superpowers?',
 '[{"text":"USA and USSR","isCorrect":true},{"text":"UK and France","isCorrect":false},{"text":"China and Japan","isCorrect":false},{"text":"Germany and Russia","isCorrect":false}]'::jsonb,
 null, 'The Cold War was primarily a political and ideological rivalry between the United States and the Soviet Union.'),
('Class 12 Arts', 'Political Science', 'The Cold War Era and Contemporary World Politics', 'medium', 'mcq',
 'The movement of countries, including India, that avoided formally aligning with either superpower bloc during the Cold War is called:',
 '[{"text":"The United Nations","isCorrect":false},{"text":"The Non-Aligned Movement","isCorrect":true},{"text":"NATO","isCorrect":false},{"text":"The Warsaw Pact","isCorrect":false}]'::jsonb,
 null, 'The Non-Aligned Movement consisted of countries that chose not to formally align with either the US or Soviet bloc during the Cold War.'),
('Class 12 Arts', 'Political Science', 'The Cold War Era and Contemporary World Politics', 'hard', 'mcq',
 'The fall of the Berlin Wall in 1989 is most closely associated with which broader historical development?',
 '[{"text":"The start of the Cold War","isCorrect":false},{"text":"The end of the Cold War and disintegration of the Soviet bloc","isCorrect":true},{"text":"World War II","isCorrect":false},{"text":"The formation of NATO","isCorrect":false}]'::jsonb,
 null, 'The fall of the Berlin Wall symbolized the end of the Cold War and the beginning of the disintegration of the Soviet bloc.'),
('Class 12 Arts', 'Political Science', 'The Cold War Era and Contemporary World Politics', 'hard', 'mcq',
 'The concept of a "unipolar world" refers to a global order where:',
 '[{"text":"Multiple superpowers share equal influence","isCorrect":false},{"text":"One single power dominates global politics","isCorrect":true},{"text":"No country has any influence","isCorrect":false},{"text":"Only regional powers exist","isCorrect":false}]'::jsonb,
 null, 'A unipolar world order is one in which a single dominant power, like the US after the Cold War, has the most significant global influence.'),

-- Political Science: Indian Politics Since Independence (Nation-Building)
('Class 12 Arts', 'Political Science', 'Indian Politics Since Independence (Nation-Building)', 'easy', 'mcq',
 'The integration of over 500 princely states into independent India was largely overseen by:',
 '[{"text":"Jawaharlal Nehru","isCorrect":false},{"text":"Sardar Vallabhbhai Patel","isCorrect":true},{"text":"Mahatma Gandhi","isCorrect":false},{"text":"Dr. Rajendra Prasad","isCorrect":false}]'::jsonb,
 null, 'Sardar Vallabhbhai Patel, as Home Minister, played the central role in integrating princely states into independent India.'),
('Class 12 Arts', 'Political Science', 'Indian Politics Since Independence (Nation-Building)', 'medium', 'mcq',
 'The reorganization of Indian states largely along linguistic lines took place mainly in which decade?',
 '[{"text":"1940s","isCorrect":false},{"text":"1950s","isCorrect":true},{"text":"1970s","isCorrect":false},{"text":"1990s","isCorrect":false}]'::jsonb,
 null, 'The States Reorganisation Act of 1956 reorganized Indian states largely along linguistic lines.'),
('Class 12 Arts', 'Political Science', 'Indian Politics Since Independence (Nation-Building)', 'hard', 'mcq',
 'The period from 1952 to the mid-1960s, marked by the Indian National Congress winning most elections with little serious opposition, is often called the era of:',
 '[{"text":"Coalition politics","isCorrect":false},{"text":"One-party dominance","isCorrect":true},{"text":"Emergency rule","isCorrect":false},{"text":"Military governance","isCorrect":false}]'::jsonb,
 null, 'This period is often called the era of one-party dominance, since the Congress won successive elections with limited serious opposition.'),
('Class 12 Arts', 'Political Science', 'Indian Politics Since Independence (Nation-Building)', 'hard', 'mcq',
 'The National Emergency declared in India in 1975 was primarily justified by the government on grounds of:',
 '[{"text":"External aggression only","isCorrect":false},{"text":"Internal disturbance","isCorrect":true},{"text":"A natural disaster","isCorrect":false},{"text":"A currency crisis","isCorrect":false}]'::jsonb,
 null, 'The 1975 National Emergency was declared under Article 352 on grounds of internal disturbance.'),

-- Geography: Human Geography: Population and Human Settlements
('Class 12 Arts', 'Geography', 'Human Geography: Population and Human Settlements', 'easy', 'mcq',
 'The branch of geography that studies the distribution, density, and growth of human population is called:',
 '[{"text":"Population geography","isCorrect":true},{"text":"Physical geography","isCorrect":false},{"text":"Cartography","isCorrect":false},{"text":"Geomorphology","isCorrect":false}]'::jsonb,
 null, 'Population geography studies the distribution, density, growth, and characteristics of human populations.'),
('Class 12 Arts', 'Geography', 'Human Geography: Population and Human Settlements', 'medium', 'mcq',
 'A settlement pattern where houses are clustered closely together, typical of many Indian villages, is called a:',
 '[{"text":"Dispersed settlement","isCorrect":false},{"text":"Compact (nucleated) settlement","isCorrect":true},{"text":"Linear settlement only","isCorrect":false},{"text":"Isolated settlement","isCorrect":false}]'::jsonb,
 null, 'A compact or nucleated settlement has houses clustered closely together, common in many Indian villages.'),
('Class 12 Arts', 'Geography', 'Human Geography: Population and Human Settlements', 'hard', 'mcq',
 'The demographic transition theory describes population change as a country moves from a stage of:',
 '[{"text":"Low birth and death rates to high birth and death rates","isCorrect":false},{"text":"High birth and death rates to low birth and death rates as it develops","isCorrect":true},{"text":"No births to only deaths","isCorrect":false},{"text":"Constant population with no change ever","isCorrect":false}]'::jsonb,
 null, 'The demographic transition theory describes how a country moves from high birth and death rates to low birth and death rates as it undergoes economic development.'),
('Class 12 Arts', 'Geography', 'Human Geography: Population and Human Settlements', 'hard', 'mcq',
 'The process by which an increasing proportion of a country''s population comes to live in urban areas is called:',
 '[{"text":"Urbanization","isCorrect":true},{"text":"Ruralization","isCorrect":false},{"text":"Migration only","isCorrect":false},{"text":"Suburbanization only","isCorrect":false}]'::jsonb,
 null, 'Urbanization refers to the increasing share of a country''s population living in urban areas over time.'),

-- Geography: India: People, Resources, Agriculture and Industries
('Class 12 Arts', 'Geography', 'India: People, Resources, Agriculture and Industries', 'easy', 'mcq',
 'Which crop is India''s largest producer of, and is also its dominant kharif (monsoon) season food crop?',
 '[{"text":"Wheat","isCorrect":false},{"text":"Rice","isCorrect":true},{"text":"Maize","isCorrect":false},{"text":"Barley","isCorrect":false}]'::jsonb,
 null, 'Rice is a major kharif crop in India, grown extensively during the monsoon season.'),
('Class 12 Arts', 'Geography', 'India: People, Resources, Agriculture and Industries', 'medium', 'mcq',
 'The agricultural revolution in India during the 1960s that significantly increased wheat and rice production through high-yielding seed varieties is called the:',
 '[{"text":"White Revolution","isCorrect":false},{"text":"Green Revolution","isCorrect":true},{"text":"Blue Revolution","isCorrect":false},{"text":"Silver Revolution","isCorrect":false}]'::jsonb,
 null, 'The Green Revolution of the 1960s significantly boosted India''s wheat and rice production through high-yielding seed varieties, fertilizers, and irrigation.'),
('Class 12 Arts', 'Geography', 'India: People, Resources, Agriculture and Industries', 'hard', 'mcq',
 'The iron and steel industry in India is heavily concentrated in states like Jharkhand, Odisha, and Chhattisgarh mainly due to:',
 '[{"text":"Coastal access to ports","isCorrect":false},{"text":"Proximity to iron ore and coal deposits","isCorrect":true},{"text":"A high concentration of skilled labor only","isCorrect":false},{"text":"Government offices being located there","isCorrect":false}]'::jsonb,
 null, 'These states have rich deposits of iron ore and coal, making them ideal locations for the iron and steel industry.'),
('Class 12 Arts', 'Geography', 'India: People, Resources, Agriculture and Industries', 'hard', 'mcq',
 'Rabi crops in India are sown in winter and harvested in:',
 '[{"text":"Summer (spring)","isCorrect":true},{"text":"Autumn","isCorrect":false},{"text":"Monsoon season","isCorrect":false},{"text":"They are not harvested seasonally","isCorrect":false}]'::jsonb,
 null, 'Rabi crops are sown in winter (October-December) and harvested in spring/summer (April-June).'),

-- English: Advanced Grammar (Modals, Voice) and Note-Making
('Class 12 Arts', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'easy', 'mcq',
 'Choose the correct modal expressing permission: "___ I leave the room, please?"',
 '[{"text":"May","isCorrect":true},{"text":"Should","isCorrect":false},{"text":"Must","isCorrect":false},{"text":"Would","isCorrect":false}]'::jsonb,
 null, '''May'' is used to politely ask for permission.'),
('Class 12 Arts', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'medium', 'mcq',
 'Change into passive voice: "The government has implemented a new policy."',
 '[{"text":"A new policy has been implemented by the government.","isCorrect":true},{"text":"A new policy is implemented by the government.","isCorrect":false},{"text":"A new policy was implemented by the government.","isCorrect":false},{"text":"A new policy has implemented by the government.","isCorrect":false}]'::jsonb,
 null, 'The present perfect active ''has implemented'' becomes ''has been implemented'' in the passive voice.'),
('Class 12 Arts', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'hard', 'mcq',
 'In note-making, the standard abbreviation for "and so on" is:',
 '[{"text":"etc.","isCorrect":true},{"text":"e.g.","isCorrect":false},{"text":"i.e.","isCorrect":false},{"text":"n.b.","isCorrect":false}]'::jsonb,
 null, '"etc." is the standard abbreviation for "and so on" or "and other similar things" in note-making.'),
('Class 12 Arts', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'hard', 'mcq',
 'Choose the correct modal expressing a criticism of a past action: "You ___ have told me earlier."',
 '[{"text":"should","isCorrect":true},{"text":"can","isCorrect":false},{"text":"may","isCorrect":false},{"text":"will","isCorrect":false}]'::jsonb,
 null, '''Should have'' expresses criticism or regret about something that was not done in the past.'),

-- English: Reading Comprehension and Vocabulary
('Class 12 Arts', 'English', 'Reading Comprehension and Vocabulary', 'easy', 'mcq',
 'Read: "The peace treaty, signed after years of conflict, brought a fragile but welcome calm to the region." What did the treaty bring?',
 '[{"text":"More conflict","isCorrect":false},{"text":"A fragile calm","isCorrect":true},{"text":"Economic collapse","isCorrect":false},{"text":"No change at all","isCorrect":false}]'::jsonb,
 null, 'The passage says the treaty brought a fragile but welcome calm to the region.'),
('Class 12 Arts', 'English', 'Reading Comprehension and Vocabulary', 'medium', 'mcq',
 'A word that means the same as ''autonomous'' is:',
 '[{"text":"Dependent","isCorrect":false},{"text":"Self-governing","isCorrect":true},{"text":"Controlled","isCorrect":false},{"text":"Weak","isCorrect":false}]'::jsonb,
 null, '''Autonomous'' means self-governing or independent.'),
('Class 12 Arts', 'English', 'Reading Comprehension and Vocabulary', 'hard', 'mcq',
 'A word that means the opposite of ''pragmatic'' is:',
 '[{"text":"Practical","isCorrect":false},{"text":"Idealistic","isCorrect":true},{"text":"Realistic","isCorrect":false},{"text":"Sensible","isCorrect":false}]'::jsonb,
 null, '''Pragmatic'' means dealing with things realistically; its opposite is ''idealistic''.'),
('Class 12 Arts', 'English', 'Reading Comprehension and Vocabulary', 'hard', 'mcq',
 'Read: "The diplomat''s carefully worded statement was widely seen as a calculated attempt to placate both sides without committing to either." What was the diplomat trying to do?',
 '[{"text":"Anger both sides","isCorrect":false},{"text":"Satisfy both sides without taking a firm position","isCorrect":true},{"text":"Take a clear, firm stance","isCorrect":false},{"text":"End all negotiations","isCorrect":false}]'::jsonb,
 null, '''Placate'' means to appease or calm; the passage says the diplomat aimed to satisfy both sides without committing to either.');
