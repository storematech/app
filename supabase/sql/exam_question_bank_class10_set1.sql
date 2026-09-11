-- supabase/sql/exam_question_bank_class10_set1.sql
--
-- First curated question-bank batch for 'Class 10' (see exam_patterns_class10.sql for the exam's
-- structure). Same exam_question_bank table/schema as the other sets -- no migration needed.
--
-- Every row here is format='mcq' with a single isCorrect:true option, matching Class 10's
-- MCQ-only, no-negative-marking pattern. 8 chapters (2 per subject x 4 subjects) each get 4
-- questions -- one easy, two medium, one hard -- reflecting the further-increased hard share in
-- exam_patterns_class10.sql. Pitched one notch above Class 9 (see
-- exam_question_bank_class9_set1.sql) -- reported speech/editing, quadratic equations/
-- trigonometry, chemical reactions/electricity-light, and Nationalism in India/power-sharing-
-- economic development, reflecting board-exam-year rigor.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns_class10.sql.

insert into exam_question_bank (exam_name, subject, chapter, difficulty, format, text, options, numerical_answer, explanation) values

-- English: Reported Speech, Editing and Error Correction
('Class 10', 'English', 'Reported Speech, Editing and Error Correction', 'easy', 'mcq',
 'Convert to reported speech: The teacher said, "The earth revolves around the sun."',
 '[{"text":"The teacher said that the earth revolved around the sun.","isCorrect":false},{"text":"The teacher said that the earth revolves around the sun.","isCorrect":true},{"text":"The teacher said the earth will revolve around the sun.","isCorrect":false},{"text":"The teacher says the earth revolves around the sun.","isCorrect":false}]'::jsonb,
 null, 'Universal truths and general facts keep their tense unchanged in reported speech.'),
('Class 10', 'English', 'Reported Speech, Editing and Error Correction', 'medium', 'mcq',
 'Identify the error: "Each of the students have submitted their assignment."',
 '[{"text":"Each","isCorrect":false},{"text":"have","isCorrect":true},{"text":"submitted","isCorrect":false},{"text":"their","isCorrect":false}]'::jsonb,
 null, '''Each'' is singular, so the verb should be ''has'', not ''have''.'),
('Class 10', 'English', 'Reported Speech, Editing and Error Correction', 'medium', 'mcq',
 'Convert to reported speech: Rahul said to me, "Can you help me with this problem?"',
 '[{"text":"Rahul asked me if I could help him with that problem.","isCorrect":true},{"text":"Rahul asked me if you can help me with this problem.","isCorrect":false},{"text":"Rahul said if I could help him with that problem.","isCorrect":false},{"text":"Rahul asked me can I help him with this problem.","isCorrect":false}]'::jsonb,
 null, 'A reported yes/no question uses ''if/whether'', shifts ''can'' to ''could'', and ''this'' to ''that''.'),
('Class 10', 'English', 'Reported Speech, Editing and Error Correction', 'hard', 'mcq',
 'Identify the error: "Neither of the answers are correct, so I need to redo the sum."',
 '[{"text":"Neither of the answers are","isCorrect":true},{"text":"is correct","isCorrect":false},{"text":"so I need","isCorrect":false},{"text":"redo the sum","isCorrect":false}]'::jsonb,
 null, '''Neither'' takes a singular verb, so it should be ''Neither of the answers is correct''.'),

-- English: Reading Comprehension and Vocabulary
('Class 10', 'English', 'Reading Comprehension and Vocabulary', 'easy', 'mcq',
 'Read: "The diplomat''s tactful response defused what could have been a tense standoff." What did the diplomat''s response do?',
 '[{"text":"Escalated the tension","isCorrect":false},{"text":"Defused the tense standoff","isCorrect":true},{"text":"Ended the negotiations","isCorrect":false},{"text":"Caused confusion","isCorrect":false}]'::jsonb,
 null, 'The passage says the diplomat''s tactful response defused what could have been a tense standoff.'),
('Class 10', 'English', 'Reading Comprehension and Vocabulary', 'medium', 'mcq',
 'A word that means the same as ''ephemeral'' is:',
 '[{"text":"Permanent","isCorrect":false},{"text":"Short-lived","isCorrect":true},{"text":"Eternal","isCorrect":false},{"text":"Ancient","isCorrect":false}]'::jsonb,
 null, '''Ephemeral'' means lasting for a very short time, matching ''short-lived''.'),
('Class 10', 'English', 'Reading Comprehension and Vocabulary', 'medium', 'mcq',
 'Read: "The author''s prose was so evocative that readers could almost smell the rain-soaked streets she described." What effect did the author''s writing have on readers?',
 '[{"text":"It bored readers","isCorrect":false},{"text":"It vividly evoked sensory imagery","isCorrect":true},{"text":"It confused readers","isCorrect":false},{"text":"It was too short to have an effect","isCorrect":false}]'::jsonb,
 null, 'The passage says the prose was so evocative that readers could almost smell the streets, showing vivid sensory imagery.'),
('Class 10', 'English', 'Reading Comprehension and Vocabulary', 'hard', 'mcq',
 'A word that means the opposite of ''austere'' is:',
 '[{"text":"Strict","isCorrect":false},{"text":"Lavish","isCorrect":true},{"text":"Plain","isCorrect":false},{"text":"Simple","isCorrect":false}]'::jsonb,
 null, '''Austere'' means severely simple or plain; its opposite is ''lavish''.'),

-- Mathematics: Real Numbers, Polynomials and Quadratic Equations
('Class 10', 'Mathematics', 'Real Numbers, Polynomials and Quadratic Equations', 'easy', 'mcq',
 'What is the HCF of 12 and 18?',
 '[{"text":"3","isCorrect":false},{"text":"6","isCorrect":true},{"text":"9","isCorrect":false},{"text":"36","isCorrect":false}]'::jsonb,
 null, 'The highest common factor of 12 and 18 is 6.'),
('Class 10', 'Mathematics', 'Real Numbers, Polynomials and Quadratic Equations', 'medium', 'mcq',
 'If the zeroes of a quadratic polynomial are 2 and 3, what is the polynomial?',
 '[{"text":"x^2 - 5x + 6","isCorrect":true},{"text":"x^2 + 5x + 6","isCorrect":false},{"text":"x^2 - 5x - 6","isCorrect":false},{"text":"x^2 + 5x - 6","isCorrect":false}]'::jsonb,
 null, 'For zeroes p and q, the polynomial is x^2 - (p+q)x + pq = x^2 - 5x + 6.'),
('Class 10', 'Mathematics', 'Real Numbers, Polynomials and Quadratic Equations', 'medium', 'mcq',
 'Solve: x^2 - 5x + 6 = 0',
 '[{"text":"x = 2, 3","isCorrect":true},{"text":"x = 1, 6","isCorrect":false},{"text":"x = -2, -3","isCorrect":false},{"text":"x = 2, -3","isCorrect":false}]'::jsonb,
 null, 'Factoring: (x-2)(x-3) = 0, so x = 2 or x = 3.'),
('Class 10', 'Mathematics', 'Real Numbers, Polynomials and Quadratic Equations', 'hard', 'mcq',
 'What is the discriminant of the quadratic equation 2x^2 - 4x + 1 = 0?',
 '[{"text":"0","isCorrect":false},{"text":"8","isCorrect":true},{"text":"16","isCorrect":false},{"text":"-8","isCorrect":false}]'::jsonb,
 null, 'Discriminant = b^2 - 4ac = (-4)^2 - 4(2)(1) = 16 - 8 = 8.'),

-- Mathematics: Trigonometry, Circles and Statistics-Probability
('Class 10', 'Mathematics', 'Trigonometry, Circles and Statistics-Probability', 'easy', 'mcq',
 'What is the value of sin(30 degrees)?',
 '[{"text":"1/2","isCorrect":true},{"text":"1","isCorrect":false},{"text":"0","isCorrect":false},{"text":"square root of 3 / 2","isCorrect":false}]'::jsonb,
 null, 'sin(30 degrees) = 1/2, a standard trigonometric value.'),
('Class 10', 'Mathematics', 'Trigonometry, Circles and Statistics-Probability', 'medium', 'mcq',
 'A tangent to a circle touches it at exactly how many points?',
 '[{"text":"0","isCorrect":false},{"text":"1","isCorrect":true},{"text":"2","isCorrect":false},{"text":"Infinite","isCorrect":false}]'::jsonb,
 null, 'A tangent to a circle touches the circle at exactly one point.'),
('Class 10', 'Mathematics', 'Trigonometry, Circles and Statistics-Probability', 'medium', 'mcq',
 'A single die is rolled once. What is the probability of getting a number greater than 4?',
 '[{"text":"1/6","isCorrect":false},{"text":"1/3","isCorrect":true},{"text":"1/2","isCorrect":false},{"text":"2/3","isCorrect":false}]'::jsonb,
 null, 'Numbers greater than 4 on a die are 5 and 6, giving 2 favorable outcomes out of 6: 2/6 = 1/3.'),
('Class 10', 'Mathematics', 'Trigonometry, Circles and Statistics-Probability', 'hard', 'mcq',
 'If tan(theta) = 3/4, what is the value of sec(theta)?',
 '[{"text":"3/5","isCorrect":false},{"text":"4/5","isCorrect":false},{"text":"5/4","isCorrect":true},{"text":"5/3","isCorrect":false}]'::jsonb,
 null, 'If tan(theta) = 3/4, the hypotenuse (using 3-4-5 triangle) is 5, so sec(theta) = hypotenuse/adjacent = 5/4.'),

-- Science: Chemical Reactions, Acids-Bases-Salts and Life Processes
('Class 10', 'Science', 'Chemical Reactions, Acids-Bases-Salts and Life Processes', 'easy', 'mcq',
 'The pH of a neutral solution is:',
 '[{"text":"0","isCorrect":false},{"text":"7","isCorrect":true},{"text":"14","isCorrect":false},{"text":"1","isCorrect":false}]'::jsonb,
 null, 'A neutral solution, such as pure water, has a pH of 7.'),
('Class 10', 'Science', 'Chemical Reactions, Acids-Bases-Salts and Life Processes', 'medium', 'mcq',
 'The process by which green plants convert light energy into chemical energy is called:',
 '[{"text":"Respiration","isCorrect":false},{"text":"Photosynthesis","isCorrect":true},{"text":"Excretion","isCorrect":false},{"text":"Transpiration","isCorrect":false}]'::jsonb,
 null, 'Photosynthesis is the process by which plants convert light energy into chemical energy stored in glucose.'),
('Class 10', 'Science', 'Chemical Reactions, Acids-Bases-Salts and Life Processes', 'medium', 'mcq',
 'A reaction in which a single compound breaks down into two or more simpler substances is called a:',
 '[{"text":"Combination reaction","isCorrect":false},{"text":"Decomposition reaction","isCorrect":true},{"text":"Displacement reaction","isCorrect":false},{"text":"Double displacement reaction","isCorrect":false}]'::jsonb,
 null, 'A decomposition reaction breaks a single compound into two or more simpler products.'),
('Class 10', 'Science', 'Chemical Reactions, Acids-Bases-Salts and Life Processes', 'hard', 'mcq',
 'Which of these salts is formed by the neutralization of a strong acid and a strong base, and is neutral in nature?',
 '[{"text":"Sodium chloride","isCorrect":true},{"text":"Sodium carbonate","isCorrect":false},{"text":"Ammonium chloride","isCorrect":false},{"text":"Copper sulphate","isCorrect":false}]'::jsonb,
 null, 'Sodium chloride, formed from strong acid HCl and strong base NaOH, is a neutral salt.'),

-- Science: Electricity, Magnetism and Light
('Class 10', 'Science', 'Electricity, Magnetism and Light', 'easy', 'mcq',
 'The SI unit of electric current is the:',
 '[{"text":"Volt","isCorrect":false},{"text":"Ampere","isCorrect":true},{"text":"Ohm","isCorrect":false},{"text":"Watt","isCorrect":false}]'::jsonb,
 null, 'The SI unit of electric current is the Ampere (A).'),
('Class 10', 'Science', 'Electricity, Magnetism and Light', 'medium', 'mcq',
 'According to Ohm''s Law, if voltage is doubled while resistance stays constant, current will:',
 '[{"text":"Remain the same","isCorrect":false},{"text":"Double","isCorrect":true},{"text":"Halve","isCorrect":false},{"text":"Become zero","isCorrect":false}]'::jsonb,
 null, 'By Ohm''s Law (V = IR), if resistance is constant, current is directly proportional to voltage, so doubling voltage doubles current.'),
('Class 10', 'Science', 'Electricity, Magnetism and Light', 'medium', 'mcq',
 'A concave mirror that converges parallel rays of light to a point is commonly used in:',
 '[{"text":"Vehicle side mirrors","isCorrect":false},{"text":"Torch/headlight reflectors","isCorrect":true},{"text":"Store security mirrors","isCorrect":false},{"text":"Sunglasses","isCorrect":false}]'::jsonb,
 null, 'Concave mirrors converge light to a point and are used in torches and headlight reflectors to focus light.'),
('Class 10', 'Science', 'Electricity, Magnetism and Light', 'hard', 'mcq',
 'Two resistors of 4 ohms and 6 ohms are connected in series. What is their total resistance?',
 '[{"text":"2.4 ohms","isCorrect":false},{"text":"10 ohms","isCorrect":true},{"text":"24 ohms","isCorrect":false},{"text":"1.5 ohms","isCorrect":false}]'::jsonb,
 null, 'In series, resistances simply add: 4 + 6 = 10 ohms.'),

-- Social Science: History (Nationalism in India) and Geography (Resources and Development)
('Class 10', 'Social Science', 'History (Nationalism in India) and Geography (Resources and Development)', 'easy', 'mcq',
 'The Non-Cooperation Movement in India was launched by Mahatma Gandhi in which year?',
 '[{"text":"1920","isCorrect":true},{"text":"1930","isCorrect":false},{"text":"1942","isCorrect":false},{"text":"1919","isCorrect":false}]'::jsonb,
 null, 'The Non-Cooperation Movement was launched by Mahatma Gandhi in 1920.'),
('Class 10', 'Social Science', 'History (Nationalism in India) and Geography (Resources and Development)', 'medium', 'mcq',
 'The Jallianwala Bagh massacre, which fueled Indian nationalism, took place in which city?',
 '[{"text":"Amritsar","isCorrect":true},{"text":"Lahore","isCorrect":false},{"text":"Delhi","isCorrect":false},{"text":"Lucknow","isCorrect":false}]'::jsonb,
 null, 'The Jallianwala Bagh massacre took place in Amritsar in 1919.'),
('Class 10', 'Social Science', 'History (Nationalism in India) and Geography (Resources and Development)', 'medium', 'mcq',
 'Resources that are limited in supply and cannot be replenished quickly, such as coal and petroleum, are called:',
 '[{"text":"Renewable resources","isCorrect":false},{"text":"Non-renewable resources","isCorrect":true},{"text":"Biotic resources","isCorrect":false},{"text":"Human resources","isCorrect":false}]'::jsonb,
 null, 'Non-renewable resources, like coal and petroleum, exist in limited quantities and take millions of years to form.'),
('Class 10', 'Social Science', 'History (Nationalism in India) and Geography (Resources and Development)', 'hard', 'mcq',
 'The Salt March (Dandi March) led by Gandhi in 1930 was a protest against which British policy?',
 '[{"text":"The salt tax and monopoly","isCorrect":true},{"text":"Partition of Bengal","isCorrect":false},{"text":"The Rowlatt Act","isCorrect":false},{"text":"Introduction of railways","isCorrect":false}]'::jsonb,
 null, 'The Salt March protested the British monopoly and tax on salt production.'),

-- Social Science: Civics (Power Sharing and Political Parties) and Economics (Development and Credit)
('Class 10', 'Social Science', 'Civics (Power Sharing and Political Parties) and Economics (Development and Credit)', 'easy', 'mcq',
 'A system where power is divided among different organs of government, such as the legislature, executive, and judiciary, is called:',
 '[{"text":"Power sharing","isCorrect":true},{"text":"Centralization","isCorrect":false},{"text":"Autocracy","isCorrect":false},{"text":"Monopoly of power","isCorrect":false}]'::jsonb,
 null, 'Power sharing refers to the distribution of power among different organs of government to prevent its concentration in one place.'),
('Class 10', 'Social Science', 'Civics (Power Sharing and Political Parties) and Economics (Development and Credit)', 'medium', 'mcq',
 'A country with only one dominant party that continuously wins elections, while other parties exist but rarely win, is said to have a:',
 '[{"text":"Two-party system","isCorrect":false},{"text":"Multi-party system","isCorrect":false},{"text":"One-party dominant system","isCorrect":true},{"text":"No party system","isCorrect":false}]'::jsonb,
 null, 'A one-party dominant system has multiple parties, but one continuously wins and dominates elections.'),
('Class 10', 'Social Science', 'Civics (Power Sharing and Political Parties) and Economics (Development and Credit)', 'medium', 'mcq',
 'Loans provided by informal sources like moneylenders usually carry which characteristic compared to formal bank loans?',
 '[{"text":"Lower interest rates","isCorrect":false},{"text":"Higher interest rates","isCorrect":true},{"text":"No interest at all","isCorrect":false},{"text":"Government regulation","isCorrect":false}]'::jsonb,
 null, 'Informal lenders like moneylenders typically charge much higher interest rates than formal banks.'),
('Class 10', 'Social Science', 'Civics (Power Sharing and Political Parties) and Economics (Development and Credit)', 'hard', 'mcq',
 'Development that meets the needs of the present without compromising the ability of future generations to meet their own needs is called:',
 '[{"text":"Economic growth","isCorrect":false},{"text":"Sustainable development","isCorrect":true},{"text":"Industrialization","isCorrect":false},{"text":"Globalization","isCorrect":false}]'::jsonb,
 null, 'Sustainable development balances present needs with the ability of future generations to meet their own needs.');
