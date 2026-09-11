-- supabase/sql/exam_question_bank_class9_set1.sql
--
-- First curated question-bank batch for 'Class 9' (see exam_patterns_class9.sql for the exam's
-- structure). Same exam_question_bank table/schema as the other sets -- no migration needed.
--
-- Every row here is format='mcq' with a single isCorrect:true option, matching Class 9's MCQ-only,
-- no-negative-marking pattern. 8 chapters (2 per subject x 4 subjects) each get 4 questions --
-- one easy, two medium, one hard -- reflecting the increased hard share in
-- exam_patterns_class9.sql. Pitched one notch above Class 8 (see
-- exam_question_bank_class8_set1.sql) -- voice/sentence transformation, polynomials/coordinate
-- geometry, atoms-molecules/gravitation, and French Revolution/democracy-economics.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns_class9.sql.

insert into exam_question_bank (exam_name, subject, chapter, difficulty, format, text, options, numerical_answer, explanation) values

-- English: Modals, Voice and Sentence Transformation
('Class 9', 'English', 'Modals, Voice and Sentence Transformation', 'easy', 'mcq',
 'Choose the correct modal: "___ I open the window, please?"',
 '[{"text":"May","isCorrect":true},{"text":"Must","isCorrect":false},{"text":"Should","isCorrect":false},{"text":"Would","isCorrect":false}]'::jsonb,
 null, '''May'' is used to politely ask for permission, correctly fitting this request.'),
('Class 9', 'English', 'Modals, Voice and Sentence Transformation', 'medium', 'mcq',
 'Change into passive voice: "The chef is cooking the meal."',
 '[{"text":"The meal is cooked by the chef.","isCorrect":false},{"text":"The meal is being cooked by the chef.","isCorrect":true},{"text":"The meal was cooked by the chef.","isCorrect":false},{"text":"The meal cooks by the chef.","isCorrect":false}]'::jsonb,
 null, 'The present continuous active ''is cooking'' becomes ''is being cooked'' in the passive voice.'),
('Class 9', 'English', 'Modals, Voice and Sentence Transformation', 'medium', 'mcq',
 'Combine using a modal of obligation: "It is necessary for students to submit the assignment on time."',
 '[{"text":"Students may submit the assignment on time.","isCorrect":false},{"text":"Students must submit the assignment on time.","isCorrect":true},{"text":"Students can submit the assignment on time.","isCorrect":false},{"text":"Students might submit the assignment on time.","isCorrect":false}]'::jsonb,
 null, '''Must'' expresses necessity or obligation, matching the meaning of "it is necessary".'),
('Class 9', 'English', 'Modals, Voice and Sentence Transformation', 'hard', 'mcq',
 'Change into passive voice: "They have completed the project."',
 '[{"text":"The project is completed by them.","isCorrect":false},{"text":"The project has been completed by them.","isCorrect":true},{"text":"The project was completed by them.","isCorrect":false},{"text":"The project had completed by them.","isCorrect":false}]'::jsonb,
 null, 'The present perfect active ''have completed'' becomes ''has been completed'' in the passive voice.'),

-- English: Reading Comprehension and Vocabulary
('Class 9', 'English', 'Reading Comprehension and Vocabulary', 'easy', 'mcq',
 'Read: "The novelist spent a decade researching before writing her acclaimed historical epic." How long did the novelist research?',
 '[{"text":"A year","isCorrect":false},{"text":"A decade","isCorrect":true},{"text":"A month","isCorrect":false},{"text":"A week","isCorrect":false}]'::jsonb,
 null, 'The passage says the novelist spent a decade researching before writing her novel.'),
('Class 9', 'English', 'Reading Comprehension and Vocabulary', 'medium', 'mcq',
 'A word that means the same as ''resilient'' is:',
 '[{"text":"Fragile","isCorrect":false},{"text":"Tough and able to recover quickly","isCorrect":true},{"text":"Weak","isCorrect":false},{"text":"Timid","isCorrect":false}]'::jsonb,
 null, '''Resilient'' means able to recover quickly from difficulties, matching ''tough and able to recover quickly''.'),
('Class 9', 'English', 'Reading Comprehension and Vocabulary', 'medium', 'mcq',
 'Read: "The policy, though well-intentioned, had unintended consequences that hurt small farmers the most." Who was most affected by the policy''s unintended consequences?',
 '[{"text":"Large corporations","isCorrect":false},{"text":"Small farmers","isCorrect":true},{"text":"Government officials","isCorrect":false},{"text":"Urban workers","isCorrect":false}]'::jsonb,
 null, 'The passage says the policy''s unintended consequences hurt small farmers the most.'),
('Class 9', 'English', 'Reading Comprehension and Vocabulary', 'hard', 'mcq',
 'A word that means the opposite of ''ambiguous'' is:',
 '[{"text":"Unclear","isCorrect":false},{"text":"Vague","isCorrect":false},{"text":"Unambiguous (clear)","isCorrect":true},{"text":"Confusing","isCorrect":false}]'::jsonb,
 null, '''Ambiguous'' means unclear or open to multiple interpretations; its opposite is ''clear'' or ''unambiguous''.'),

-- Mathematics: Number Systems and Polynomials
('Class 9', 'Mathematics', 'Number Systems and Polynomials', 'easy', 'mcq',
 'Which of the following is an irrational number?',
 '[{"text":"4","isCorrect":false},{"text":"1/2","isCorrect":false},{"text":"Square root of 2","isCorrect":true},{"text":"0.5","isCorrect":false}]'::jsonb,
 null, 'The square root of 2 cannot be expressed as a simple fraction, making it irrational.'),
('Class 9', 'Mathematics', 'Number Systems and Polynomials', 'medium', 'mcq',
 'What is the degree of the polynomial 5x^3 + 2x^2 - x + 7?',
 '[{"text":"1","isCorrect":false},{"text":"2","isCorrect":false},{"text":"3","isCorrect":true},{"text":"7","isCorrect":false}]'::jsonb,
 null, 'The degree of a polynomial is the highest power of the variable, which is 3 here.'),
('Class 9', 'Mathematics', 'Number Systems and Polynomials', 'medium', 'mcq',
 'If p(x) = x^2 - 4, what is the value of p(2)?',
 '[{"text":"0","isCorrect":true},{"text":"2","isCorrect":false},{"text":"4","isCorrect":false},{"text":"-4","isCorrect":false}]'::jsonb,
 null, 'p(2) = 2^2 - 4 = 4 - 4 = 0.'),
('Class 9', 'Mathematics', 'Number Systems and Polynomials', 'hard', 'mcq',
 'Factorize: x^2 - 9',
 '[{"text":"(x-3)(x-3)","isCorrect":false},{"text":"(x+3)(x-3)","isCorrect":true},{"text":"(x+9)(x-1)","isCorrect":false},{"text":"(x-9)(x+1)","isCorrect":false}]'::jsonb,
 null, 'x^2 - 9 is a difference of squares: x^2 - 3^2 = (x+3)(x-3).'),

-- Mathematics: Coordinate Geometry, Linear Equations and Mensuration
('Class 9', 'Mathematics', 'Coordinate Geometry, Linear Equations and Mensuration', 'easy', 'mcq',
 'In which quadrant does the point (-3, 4) lie?',
 '[{"text":"First","isCorrect":false},{"text":"Second","isCorrect":true},{"text":"Third","isCorrect":false},{"text":"Fourth","isCorrect":false}]'::jsonb,
 null, 'A point with a negative x-coordinate and positive y-coordinate lies in the second quadrant.'),
('Class 9', 'Mathematics', 'Coordinate Geometry, Linear Equations and Mensuration', 'medium', 'mcq',
 'Which of these points lies on the line y = 2x + 1?',
 '[{"text":"(1, 3)","isCorrect":true},{"text":"(1, 2)","isCorrect":false},{"text":"(2, 3)","isCorrect":false},{"text":"(0, 0)","isCorrect":false}]'::jsonb,
 null, 'Substituting x=1: y = 2(1) + 1 = 3, so (1, 3) satisfies the equation.'),
('Class 9', 'Mathematics', 'Coordinate Geometry, Linear Equations and Mensuration', 'medium', 'mcq',
 'What is the curved surface area of a cylinder with radius 7 cm and height 10 cm? (use pi = 22/7)',
 '[{"text":"220 cm^2","isCorrect":false},{"text":"440 cm^2","isCorrect":true},{"text":"110 cm^2","isCorrect":false},{"text":"880 cm^2","isCorrect":false}]'::jsonb,
 null, 'Curved surface area of a cylinder = 2 x pi x r x h = 2 x (22/7) x 7 x 10 = 440 cm^2.'),
('Class 9', 'Mathematics', 'Coordinate Geometry, Linear Equations and Mensuration', 'hard', 'mcq',
 'What is the total surface area of a cube with side 6 cm?',
 '[{"text":"36 cm^2","isCorrect":false},{"text":"144 cm^2","isCorrect":false},{"text":"216 cm^2","isCorrect":true},{"text":"196 cm^2","isCorrect":false}]'::jsonb,
 null, 'Total surface area of a cube = 6 x side^2 = 6 x 36 = 216 cm^2.'),

-- Science: Matter, Atoms-Molecules and Cell Structure
('Class 9', 'Science', 'Matter, Atoms-Molecules and Cell Structure', 'easy', 'mcq',
 'The process by which a solid changes directly into a gas without passing through the liquid state is called:',
 '[{"text":"Evaporation","isCorrect":false},{"text":"Sublimation","isCorrect":true},{"text":"Condensation","isCorrect":false},{"text":"Melting","isCorrect":false}]'::jsonb,
 null, 'Sublimation is the direct change of a solid to a gas without becoming liquid first.'),
('Class 9', 'Science', 'Matter, Atoms-Molecules and Cell Structure', 'medium', 'mcq',
 'The smallest particle of an element that can take part in a chemical reaction is called a(n):',
 '[{"text":"Molecule","isCorrect":false},{"text":"Atom","isCorrect":true},{"text":"Ion","isCorrect":false},{"text":"Compound","isCorrect":false}]'::jsonb,
 null, 'An atom is the smallest particle of an element that retains the properties needed to take part in chemical reactions.'),
('Class 9', 'Science', 'Matter, Atoms-Molecules and Cell Structure', 'medium', 'mcq',
 'Which cell organelle is responsible for protein synthesis?',
 '[{"text":"Ribosome","isCorrect":true},{"text":"Mitochondria","isCorrect":false},{"text":"Golgi body","isCorrect":false},{"text":"Lysosome","isCorrect":false}]'::jsonb,
 null, 'Ribosomes are the site of protein synthesis in a cell.'),
('Class 9', 'Science', 'Matter, Atoms-Molecules and Cell Structure', 'hard', 'mcq',
 'An atom with 11 protons and 12 neutrons has a mass number of:',
 '[{"text":"11","isCorrect":false},{"text":"12","isCorrect":false},{"text":"23","isCorrect":true},{"text":"1","isCorrect":false}]'::jsonb,
 null, 'Mass number = number of protons + number of neutrons = 11 + 12 = 23.'),

-- Science: Motion, Force, Gravitation and Sound
('Class 9', 'Science', 'Motion, Force, Gravitation and Sound', 'easy', 'mcq',
 'The SI unit of force is the:',
 '[{"text":"Joule","isCorrect":false},{"text":"Newton","isCorrect":true},{"text":"Watt","isCorrect":false},{"text":"Pascal","isCorrect":false}]'::jsonb,
 null, 'The SI unit of force is the Newton (N).'),
('Class 9', 'Science', 'Motion, Force, Gravitation and Sound', 'medium', 'mcq',
 'If a car travels 150 km in 3 hours, what is its average speed?',
 '[{"text":"40 km/h","isCorrect":false},{"text":"50 km/h","isCorrect":true},{"text":"60 km/h","isCorrect":false},{"text":"45 km/h","isCorrect":false}]'::jsonb,
 null, 'Average speed = distance / time = 150 / 3 = 50 km/h.'),
('Class 9', 'Science', 'Motion, Force, Gravitation and Sound', 'medium', 'mcq',
 'The force of attraction between the Earth and any object near it is called:',
 '[{"text":"Magnetic force","isCorrect":false},{"text":"Gravitational force","isCorrect":true},{"text":"Frictional force","isCorrect":false},{"text":"Electrostatic force","isCorrect":false}]'::jsonb,
 null, 'Gravitational force is the force of attraction between the Earth and any object near it.'),
('Class 9', 'Science', 'Motion, Force, Gravitation and Sound', 'hard', 'mcq',
 'A body of mass 10 kg experiences a force of 20 N. What is its acceleration?',
 '[{"text":"0.5 m/s^2","isCorrect":false},{"text":"2 m/s^2","isCorrect":true},{"text":"10 m/s^2","isCorrect":false},{"text":"200 m/s^2","isCorrect":false}]'::jsonb,
 null, 'Using F = ma: a = F/m = 20/10 = 2 m/s^2.'),

-- Social Science: History (French Revolution) and Geography (Physical Features of India)
('Class 9', 'Social Science', 'History (French Revolution) and Geography (Physical Features of India)', 'easy', 'mcq',
 'The French Revolution began in which year?',
 '[{"text":"1789","isCorrect":true},{"text":"1799","isCorrect":false},{"text":"1776","isCorrect":false},{"text":"1804","isCorrect":false}]'::jsonb,
 null, 'The French Revolution began in 1789.'),
('Class 9', 'Social Science', 'History (French Revolution) and Geography (Physical Features of India)', 'medium', 'mcq',
 'Which mountain range in India is the youngest and still rising due to tectonic activity?',
 '[{"text":"Aravalli","isCorrect":false},{"text":"Himalayas","isCorrect":true},{"text":"Western Ghats","isCorrect":false},{"text":"Vindhya","isCorrect":false}]'::jsonb,
 null, 'The Himalayas are geologically young fold mountains that are still rising due to ongoing tectonic activity.'),
('Class 9', 'Social Science', 'History (French Revolution) and Geography (Physical Features of India)', 'medium', 'mcq',
 'The French Revolutionary ideal of "Liberty, Equality, Fraternity" is most closely associated with which document?',
 '[{"text":"Magna Carta","isCorrect":false},{"text":"Declaration of the Rights of Man and Citizen","isCorrect":true},{"text":"Treaty of Versailles","isCorrect":false},{"text":"US Constitution","isCorrect":false}]'::jsonb,
 null, 'The Declaration of the Rights of Man and Citizen (1789) enshrined the ideals of Liberty, Equality, and Fraternity.'),
('Class 9', 'Social Science', 'History (French Revolution) and Geography (Physical Features of India)', 'hard', 'mcq',
 'The northern plains of India, formed by the deposition of silt by rivers, are known as the:',
 '[{"text":"Deccan Plateau","isCorrect":false},{"text":"Indo-Gangetic Plains","isCorrect":true},{"text":"Coastal Plains","isCorrect":false},{"text":"Thar Desert","isCorrect":false}]'::jsonb,
 null, 'The Indo-Gangetic Plains were formed by the deposition of silt carried by rivers like the Ganga, Yamuna, and Brahmaputra.'),

-- Social Science: Civics (Democracy) and Economics (Poverty and Livelihood)
('Class 9', 'Social Science', 'Civics (Democracy) and Economics (Poverty and Livelihood)', 'easy', 'mcq',
 'In a democracy, the government derives its authority mainly from:',
 '[{"text":"The military","isCorrect":false},{"text":"The people","isCorrect":true},{"text":"A monarch","isCorrect":false},{"text":"A single ruling party without elections","isCorrect":false}]'::jsonb,
 null, 'In a democracy, the government derives its authority from the people through free and fair elections.'),
('Class 9', 'Social Science', 'Civics (Democracy) and Economics (Poverty and Livelihood)', 'medium', 'mcq',
 'The minimum level of income needed to meet basic needs, used to measure poverty, is called the:',
 '[{"text":"Poverty line","isCorrect":true},{"text":"Wage ceiling","isCorrect":false},{"text":"Income tax slab","isCorrect":false},{"text":"Inflation rate","isCorrect":false}]'::jsonb,
 null, 'The poverty line is the minimum income level considered necessary to meet a person''s basic needs.'),
('Class 9', 'Social Science', 'Civics (Democracy) and Economics (Poverty and Livelihood)', 'medium', 'mcq',
 'Which of these is a key feature that distinguishes a democracy from other forms of government?',
 '[{"text":"Regular, free and fair elections","isCorrect":true},{"text":"Rule by one family","isCorrect":false},{"text":"No elections held","isCorrect":false},{"text":"Rule by the military","isCorrect":false}]'::jsonb,
 null, 'Regular, free and fair elections that allow citizens to choose their representatives are a key feature of democracy.'),
('Class 9', 'Social Science', 'Civics (Democracy) and Economics (Poverty and Livelihood)', 'hard', 'mcq',
 'A situation where a country''s poor face deprivation in multiple aspects such as health, education, and living standards at once is best described as:',
 '[{"text":"Relative poverty only","isCorrect":false},{"text":"Multidimensional poverty","isCorrect":true},{"text":"Income inequality only","isCorrect":false},{"text":"Frictional unemployment","isCorrect":false}]'::jsonb,
 null, 'Multidimensional poverty captures deprivation across several aspects of life at once, not just low income.');
