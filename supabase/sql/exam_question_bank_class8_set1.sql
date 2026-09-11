-- supabase/sql/exam_question_bank_class8_set1.sql
--
-- First curated question-bank batch for 'Class 8' (see exam_patterns_class8.sql for the exam's
-- structure). Same exam_question_bank table/schema as the other sets -- no migration needed.
--
-- Every row here is format='mcq' with a single isCorrect:true option, matching Class 8's MCQ-only,
-- no-negative-marking pattern. 8 chapters (2 per subject x 4 subjects) each get 4 questions --
-- one easy, two medium, one hard -- reflecting the increased hard share in
-- exam_patterns_class8.sql. Pitched one notch above Class 7 (see
-- exam_question_bank_class7_set1.sql) -- modals/subject-verb agreement, exponents/linear
-- equations, cell structure/force-friction/chemical effects of current, and Company Rule-1857
-- Revolt/Constitution-judiciary.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns_class8.sql.

insert into exam_question_bank (exam_name, subject, chapter, difficulty, format, text, options, numerical_answer, explanation) values

-- English: Modals, Determiners and Subject-Verb Agreement
('Class 8', 'English', 'Modals, Determiners and Subject-Verb Agreement', 'easy', 'mcq',
 'Choose the correct modal: "You ___ finish your homework before playing."',
 '[{"text":"must","isCorrect":true},{"text":"might","isCorrect":false},{"text":"could","isCorrect":false},{"text":"may","isCorrect":false}]'::jsonb,
 null, '''Must'' expresses a strong obligation, correctly showing homework needs to be finished first.'),
('Class 8', 'English', 'Modals, Determiners and Subject-Verb Agreement', 'medium', 'mcq',
 'Choose the correct determiner: "There isn''t ___ milk left in the fridge."',
 '[{"text":"many","isCorrect":false},{"text":"much","isCorrect":true},{"text":"few","isCorrect":false},{"text":"a few","isCorrect":false}]'::jsonb,
 null, '''Much'' is used with uncountable nouns like ''milk'', while ''many'' and ''few'' are used with countable nouns.'),
('Class 8', 'English', 'Modals, Determiners and Subject-Verb Agreement', 'medium', 'mcq',
 'Choose the correct verb: "Neither the teacher nor the students ___ aware of the change."',
 '[{"text":"was","isCorrect":false},{"text":"is","isCorrect":false},{"text":"were","isCorrect":true},{"text":"has been","isCorrect":false}]'::jsonb,
 null, 'With ''neither...nor'', the verb agrees with the subject closer to it -- ''students'' (plural), so ''were'' is correct.'),
('Class 8', 'English', 'Modals, Determiners and Subject-Verb Agreement', 'hard', 'mcq',
 'Choose the correct modal expressing a past possibility: "She ___ have missed the bus."',
 '[{"text":"must","isCorrect":false},{"text":"might","isCorrect":true},{"text":"shall","isCorrect":false},{"text":"will","isCorrect":false}]'::jsonb,
 null, '''Might have'' expresses an uncertain possibility about the past, correctly fitting the sentence.'),

-- English: Reading Comprehension and Vocabulary
('Class 8', 'English', 'Reading Comprehension and Vocabulary', 'easy', 'mcq',
 'Read: "Despite facing repeated failures, the inventor refused to abandon his project." What did the inventor refuse to do?',
 '[{"text":"Abandon his project","isCorrect":true},{"text":"Continue his project","isCorrect":false},{"text":"Sell his invention","isCorrect":false},{"text":"Share his research","isCorrect":false}]'::jsonb,
 null, 'The passage says the inventor refused to abandon his project despite repeated failures.'),
('Class 8', 'English', 'Reading Comprehension and Vocabulary', 'medium', 'mcq',
 'A word that means the same as ''pragmatic'' is:',
 '[{"text":"Idealistic","isCorrect":false},{"text":"Practical","isCorrect":true},{"text":"Emotional","isCorrect":false},{"text":"Careless","isCorrect":false}]'::jsonb,
 null, '''Pragmatic'' means dealing with things sensibly and realistically, the same as ''practical''.'),
('Class 8', 'English', 'Reading Comprehension and Vocabulary', 'medium', 'mcq',
 'Read: "The committee deliberated for hours before reaching a unanimous decision." What does ''unanimous'' suggest about the decision?',
 '[{"text":"Everyone agreed","isCorrect":true},{"text":"No one agreed","isCorrect":false},{"text":"Only one person decided","isCorrect":false},{"text":"The decision was postponed","isCorrect":false}]'::jsonb,
 null, '''Unanimous'' means everyone was in complete agreement about the decision.'),
('Class 8', 'English', 'Reading Comprehension and Vocabulary', 'hard', 'mcq',
 'A word that means the opposite of ''concise'' is:',
 '[{"text":"Brief","isCorrect":false},{"text":"Verbose","isCorrect":true},{"text":"Short","isCorrect":false},{"text":"Clear","isCorrect":false}]'::jsonb,
 null, '''Verbose'' means using more words than necessary, the opposite of ''concise''.'),

-- Mathematics: Rational Numbers, Exponents and Squares-Square Roots
('Class 8', 'Mathematics', 'Rational Numbers, Exponents and Squares-Square Roots', 'easy', 'mcq',
 'What is 2^5?',
 '[{"text":"10","isCorrect":false},{"text":"25","isCorrect":false},{"text":"32","isCorrect":true},{"text":"16","isCorrect":false}]'::jsonb,
 null, '2^5 = 2 x 2 x 2 x 2 x 2 = 32.'),
('Class 8', 'Mathematics', 'Rational Numbers, Exponents and Squares-Square Roots', 'medium', 'mcq',
 'What is the square root of 169?',
 '[{"text":"11","isCorrect":false},{"text":"12","isCorrect":false},{"text":"13","isCorrect":true},{"text":"14","isCorrect":false}]'::jsonb,
 null, '13 x 13 = 169, so the square root of 169 is 13.'),
('Class 8', 'Mathematics', 'Rational Numbers, Exponents and Squares-Square Roots', 'medium', 'mcq',
 'What is the additive inverse of -5/7?',
 '[{"text":"5/7","isCorrect":true},{"text":"-5/7","isCorrect":false},{"text":"7/5","isCorrect":false},{"text":"-7/5","isCorrect":false}]'::jsonb,
 null, 'The additive inverse of a number, when added to it, gives zero. -5/7 + 5/7 = 0, so 5/7 is the additive inverse.'),
('Class 8', 'Mathematics', 'Rational Numbers, Exponents and Squares-Square Roots', 'hard', 'mcq',
 'Simplify: (3^2 x 3^4) / 3^3',
 '[{"text":"3^2","isCorrect":false},{"text":"3^3","isCorrect":true},{"text":"3^5","isCorrect":false},{"text":"3^9","isCorrect":false}]'::jsonb,
 null, 'Using exponent rules: (3^2 x 3^4) / 3^3 = 3^(2+4-3) = 3^3.'),

-- Mathematics: Linear Equations, Mensuration and Data Handling
('Class 8', 'Mathematics', 'Linear Equations, Mensuration and Data Handling', 'easy', 'mcq',
 'Solve for x: 2x - 4 = 10',
 '[{"text":"x = 5","isCorrect":false},{"text":"x = 7","isCorrect":true},{"text":"x = 6","isCorrect":false},{"text":"x = 3","isCorrect":false}]'::jsonb,
 null, '2x - 4 = 10 gives 2x = 14, so x = 7.'),
('Class 8', 'Mathematics', 'Linear Equations, Mensuration and Data Handling', 'medium', 'mcq',
 'What is the area of a trapezium with parallel sides 8 cm and 12 cm and height 5 cm?',
 '[{"text":"40 cm^2","isCorrect":false},{"text":"50 cm^2","isCorrect":true},{"text":"60 cm^2","isCorrect":false},{"text":"100 cm^2","isCorrect":false}]'::jsonb,
 null, 'Area of a trapezium = (1/2) x (sum of parallel sides) x height = (1/2) x 20 x 5 = 50 cm^2.'),
('Class 8', 'Mathematics', 'Linear Equations, Mensuration and Data Handling', 'medium', 'mcq',
 'In a pie chart, if a sector represents 25% of the total, what is its angle?',
 '[{"text":"45 degrees","isCorrect":false},{"text":"90 degrees","isCorrect":true},{"text":"120 degrees","isCorrect":false},{"text":"180 degrees","isCorrect":false}]'::jsonb,
 null, '25% of the full circle (360 degrees) = (25/100) x 360 = 90 degrees.'),
('Class 8', 'Mathematics', 'Linear Equations, Mensuration and Data Handling', 'hard', 'mcq',
 'Solve for x: (2x + 3)/5 = 3',
 '[{"text":"x = 5","isCorrect":false},{"text":"x = 6","isCorrect":true},{"text":"x = 7","isCorrect":false},{"text":"x = 9","isCorrect":false}]'::jsonb,
 null, '(2x + 3)/5 = 3 gives 2x + 3 = 15, so 2x = 12, and x = 6.'),

-- Science: Crop Production, Microorganisms and Cell Structure
('Class 8', 'Science', 'Crop Production, Microorganisms and Cell Structure', 'easy', 'mcq',
 'The basic structural and functional unit of all living organisms is the:',
 '[{"text":"Tissue","isCorrect":false},{"text":"Cell","isCorrect":true},{"text":"Organ","isCorrect":false},{"text":"Organelle","isCorrect":false}]'::jsonb,
 null, 'The cell is the basic structural and functional unit of all living organisms.'),
('Class 8', 'Science', 'Crop Production, Microorganisms and Cell Structure', 'medium', 'mcq',
 'Which microorganism is used to make curd from milk?',
 '[{"text":"Virus","isCorrect":false},{"text":"Bacteria","isCorrect":true},{"text":"Protozoa","isCorrect":false},{"text":"Algae","isCorrect":false}]'::jsonb,
 null, 'Lactobacillus bacteria convert milk into curd through fermentation.'),
('Class 8', 'Science', 'Crop Production, Microorganisms and Cell Structure', 'medium', 'mcq',
 'Which cell organelle is known as the "powerhouse of the cell"?',
 '[{"text":"Nucleus","isCorrect":false},{"text":"Mitochondria","isCorrect":true},{"text":"Ribosome","isCorrect":false},{"text":"Golgi body","isCorrect":false}]'::jsonb,
 null, 'Mitochondria generate energy for the cell, earning them the name "powerhouse of the cell".'),
('Class 8', 'Science', 'Crop Production, Microorganisms and Cell Structure', 'hard', 'mcq',
 'The process of growing the same crop repeatedly on the same land, which depletes specific soil nutrients, is best addressed by:',
 '[{"text":"Crop rotation","isCorrect":true},{"text":"Overirrigation","isCorrect":false},{"text":"Deforestation","isCorrect":false},{"text":"Excessive plowing","isCorrect":false}]'::jsonb,
 null, 'Crop rotation, growing different crops in sequence, helps restore soil nutrients depleted by repeated single-crop farming.'),

-- Science: Force, Friction, Sound and Chemical Effects of Current
('Class 8', 'Science', 'Force, Friction, Sound and Chemical Effects of Current', 'easy', 'mcq',
 'The force that opposes the relative motion between two surfaces in contact is called:',
 '[{"text":"Gravity","isCorrect":false},{"text":"Friction","isCorrect":true},{"text":"Magnetism","isCorrect":false},{"text":"Tension","isCorrect":false}]'::jsonb,
 null, 'Friction is the force that opposes relative motion between two surfaces in contact.'),
('Class 8', 'Science', 'Force, Friction, Sound and Chemical Effects of Current', 'medium', 'mcq',
 'The number of vibrations per second of a sound wave is called its:',
 '[{"text":"Amplitude","isCorrect":false},{"text":"Frequency","isCorrect":true},{"text":"Wavelength","isCorrect":false},{"text":"Pitch level","isCorrect":false}]'::jsonb,
 null, 'Frequency is the number of vibrations (oscillations) a sound wave completes per second.'),
('Class 8', 'Science', 'Force, Friction, Sound and Chemical Effects of Current', 'medium', 'mcq',
 'The deposition of a layer of metal on another metal using electricity is called:',
 '[{"text":"Electroplating","isCorrect":true},{"text":"Electrolysis","isCorrect":false},{"text":"Galvanization","isCorrect":false},{"text":"Oxidation","isCorrect":false}]'::jsonb,
 null, 'Electroplating uses electric current to deposit a thin layer of one metal onto another.'),
('Class 8', 'Science', 'Force, Friction, Sound and Chemical Effects of Current', 'hard', 'mcq',
 'Which type of friction acts on an object that is already moving, opposing its continued motion?',
 '[{"text":"Static friction","isCorrect":false},{"text":"Kinetic friction","isCorrect":true},{"text":"Rolling friction only","isCorrect":false},{"text":"No friction acts on moving objects","isCorrect":false}]'::jsonb,
 null, 'Kinetic (or sliding) friction acts on an object that is already in motion, opposing its continued movement.'),

-- Social Science: History (Company Rule to 1857 Revolt) and Geography (Resources)
('Class 8', 'Social Science', 'History (Company Rule to 1857 Revolt) and Geography (Resources)', 'easy', 'mcq',
 'The 1857 Revolt against British rule began at which garrison town?',
 '[{"text":"Meerut","isCorrect":true},{"text":"Delhi","isCorrect":false},{"text":"Kanpur","isCorrect":false},{"text":"Lucknow","isCorrect":false}]'::jsonb,
 null, 'The 1857 Revolt began at Meerut, where Indian soldiers (sepoys) rebelled against the British East India Company.'),
('Class 8', 'Social Science', 'History (Company Rule to 1857 Revolt) and Geography (Resources)', 'medium', 'mcq',
 'The system introduced by the British in Bengal, where zamindars collected taxes from peasants, was called the:',
 '[{"text":"Ryotwari System","isCorrect":false},{"text":"Permanent Settlement","isCorrect":true},{"text":"Mahalwari System","isCorrect":false},{"text":"Doctrine of Lapse","isCorrect":false}]'::jsonb,
 null, 'The Permanent Settlement (1793) fixed the revenue that zamindars had to pay, giving them the right to collect taxes from peasants.'),
('Class 8', 'Social Science', 'History (Company Rule to 1857 Revolt) and Geography (Resources)', 'medium', 'mcq',
 'Resources that can be replenished naturally over time, such as forests and water, are called:',
 '[{"text":"Non-renewable resources","isCorrect":false},{"text":"Renewable resources","isCorrect":true},{"text":"Human resources","isCorrect":false},{"text":"Abiotic resources only","isCorrect":false}]'::jsonb,
 null, 'Renewable resources, like forests and water, can be replenished naturally over time.'),
('Class 8', 'Social Science', 'History (Company Rule to 1857 Revolt) and Geography (Resources)', 'hard', 'mcq',
 'The policy under which the British East India Company annexed a princely state if its ruler died without a natural heir was called the:',
 '[{"text":"Subsidiary Alliance","isCorrect":false},{"text":"Doctrine of Lapse","isCorrect":true},{"text":"Permanent Settlement","isCorrect":false},{"text":"Ryotwari System","isCorrect":false}]'::jsonb,
 null, 'The Doctrine of Lapse allowed the British to annex a princely state if its ruler died without a natural heir.'),

-- Social Science: Civics (Indian Constitution, Judiciary and Social Justice)
('Class 8', 'Social Science', 'Civics (Indian Constitution, Judiciary and Social Justice)', 'easy', 'mcq',
 'The Indian Constitution came into effect on which date?',
 '[{"text":"15 August 1947","isCorrect":false},{"text":"26 January 1950","isCorrect":true},{"text":"26 November 1949","isCorrect":false},{"text":"2 October 1950","isCorrect":false}]'::jsonb,
 null, 'The Indian Constitution came into effect on 26 January 1950, celebrated as Republic Day.'),
('Class 8', 'Social Science', 'Civics (Indian Constitution, Judiciary and Social Justice)', 'medium', 'mcq',
 'The highest court in India, with the power to interpret the Constitution, is the:',
 '[{"text":"High Court","isCorrect":false},{"text":"Supreme Court","isCorrect":true},{"text":"District Court","isCorrect":false},{"text":"Parliament","isCorrect":false}]'::jsonb,
 null, 'The Supreme Court is India''s highest court, with the final authority to interpret the Constitution.'),
('Class 8', 'Social Science', 'Civics (Indian Constitution, Judiciary and Social Justice)', 'medium', 'mcq',
 'A citizen''s right to seek justice if their fundamental rights are violated is protected under which fundamental right?',
 '[{"text":"Right to Equality","isCorrect":false},{"text":"Right to Constitutional Remedies","isCorrect":true},{"text":"Right to Freedom","isCorrect":false},{"text":"Right against Exploitation","isCorrect":false}]'::jsonb,
 null, 'The Right to Constitutional Remedies allows citizens to approach courts if their fundamental rights are violated.'),
('Class 8', 'Social Science', 'Civics (Indian Constitution, Judiciary and Social Justice)', 'hard', 'mcq',
 'The independence of the judiciary from the legislature and executive is important mainly because it ensures:',
 '[{"text":"Faster court proceedings","isCorrect":false},{"text":"Fair and impartial justice","isCorrect":true},{"text":"Lower court fees","isCorrect":false},{"text":"More judges are appointed","isCorrect":false}]'::jsonb,
 null, 'An independent judiciary can deliver fair and impartial justice without pressure from the legislature or executive.');
