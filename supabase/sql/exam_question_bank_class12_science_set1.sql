-- supabase/sql/exam_question_bank_class12_science_set1.sql
--
-- First curated question-bank batch for 'Class 12 Science' (see exam_patterns_class12_science.sql
-- for the exam's structure). Same exam_question_bank table/schema as the other sets -- no
-- migration needed.
--
-- Every row here is format='mcq' with a single isCorrect:true option, matching this exam's
-- MCQ-only, no-negative-marking pattern. 8 chapters (2 per subject x 4 subjects: Physics,
-- Chemistry, Mathematics, English) each get 4 questions -- one easy, one medium, two hard --
-- reflecting the final-board-year hard share in exam_patterns_class12_science.sql. Pitched one
-- notch above Class 11 Science (see exam_question_bank_class11_science_set1.sql).
--
-- Run this once in the Supabase SQL Editor, after exam_patterns_class12_science.sql.

insert into exam_question_bank (exam_name, subject, chapter, difficulty, format, text, options, numerical_answer, explanation) values

-- Physics: Electrostatics and Current Electricity
('Class 12 Science', 'Physics', 'Electrostatics and Current Electricity', 'easy', 'mcq',
 'The SI unit of electric charge is the:',
 '[{"text":"Ampere","isCorrect":false},{"text":"Coulomb","isCorrect":true},{"text":"Volt","isCorrect":false},{"text":"Ohm","isCorrect":false}]'::jsonb,
 null, 'The SI unit of electric charge is the Coulomb (C).'),
('Class 12 Science', 'Physics', 'Electrostatics and Current Electricity', 'medium', 'mcq',
 'Two point charges of +2 uC and -2 uC are placed 1 m apart. What is the nature of the force between them?',
 '[{"text":"Repulsive","isCorrect":false},{"text":"Attractive","isCorrect":true},{"text":"Zero","isCorrect":false},{"text":"Cannot be determined","isCorrect":false}]'::jsonb,
 null, 'Unlike charges (positive and negative) attract each other.'),
('Class 12 Science', 'Physics', 'Electrostatics and Current Electricity', 'hard', 'mcq',
 'Three resistors of 2 ohms, 3 ohms, and 6 ohms are connected in parallel. What is their equivalent resistance?',
 '[{"text":"1 ohm","isCorrect":true},{"text":"11 ohms","isCorrect":false},{"text":"2 ohms","isCorrect":false},{"text":"0.5 ohms","isCorrect":false}]'::jsonb,
 null, '1/R = 1/2 + 1/3 + 1/6 = 3/6 + 2/6 + 1/6 = 6/6 = 1, so R = 1 ohm.'),
('Class 12 Science', 'Physics', 'Electrostatics and Current Electricity', 'hard', 'mcq',
 'The electric field at a distance r from a point charge q, in a medium of permittivity epsilon, is given by:',
 '[{"text":"kq/r","isCorrect":false},{"text":"kq/r^2","isCorrect":true},{"text":"kq^2/r","isCorrect":false},{"text":"kqr","isCorrect":false}]'::jsonb,
 null, 'By Coulomb''s law, the electric field due to a point charge is E = kq/r^2, where k = 1/(4 pi epsilon).'),

-- Physics: Magnetism-EM Induction, and Optics-Modern Physics
('Class 12 Science', 'Physics', 'Magnetism-EM Induction, and Optics-Modern Physics', 'easy', 'mcq',
 'The phenomenon of generating an EMF due to a changing magnetic flux is called:',
 '[{"text":"Electrostatic induction","isCorrect":false},{"text":"Electromagnetic induction","isCorrect":true},{"text":"Magnetostriction","isCorrect":false},{"text":"Photoelectric effect","isCorrect":false}]'::jsonb,
 null, 'Electromagnetic induction is the generation of an EMF due to a changing magnetic flux, discovered by Faraday.'),
('Class 12 Science', 'Physics', 'Magnetism-EM Induction, and Optics-Modern Physics', 'medium', 'mcq',
 'A convex lens forms a real, inverted image when the object is placed:',
 '[{"text":"Between the lens and its focus","isCorrect":false},{"text":"Beyond the focal length","isCorrect":true},{"text":"At the optical center","isCorrect":false},{"text":"At infinity only","isCorrect":false}]'::jsonb,
 null, 'A convex lens forms a real, inverted image when the object is placed beyond the focal length.'),
('Class 12 Science', 'Physics', 'Magnetism-EM Induction, and Optics-Modern Physics', 'hard', 'mcq',
 'In the photoelectric effect, increasing the intensity of incident light (keeping frequency constant) causes:',
 '[{"text":"An increase in the number of photoelectrons emitted","isCorrect":true},{"text":"An increase in the maximum kinetic energy of photoelectrons","isCorrect":false},{"text":"No photoelectrons to be emitted","isCorrect":false},{"text":"A decrease in the number of photoelectrons","isCorrect":false}]'::jsonb,
 null, 'Increasing intensity at constant frequency increases the number of photons, and hence the number of photoelectrons emitted, not their maximum kinetic energy.'),
('Class 12 Science', 'Physics', 'Magnetism-EM Induction, and Optics-Modern Physics', 'hard', 'mcq',
 'According to Lenz''s law, the direction of an induced current always opposes:',
 '[{"text":"The applied magnetic field only","isCorrect":false},{"text":"The change in magnetic flux that produced it","isCorrect":true},{"text":"The direction of the conductor''s motion only","isCorrect":false},{"text":"Nothing in particular","isCorrect":false}]'::jsonb,
 null, 'Lenz''s law states that the induced current opposes the change in magnetic flux that caused it, consistent with the conservation of energy.'),

-- Chemistry: Solid State, Solutions and Electrochemistry
('Class 12 Science', 'Chemistry', 'Solid State, Solutions and Electrochemistry', 'easy', 'mcq',
 'A solid in which particles are arranged in a regular, repeating pattern is called a:',
 '[{"text":"Amorphous solid","isCorrect":false},{"text":"Crystalline solid","isCorrect":true},{"text":"Colloid","isCorrect":false},{"text":"Gel","isCorrect":false}]'::jsonb,
 null, 'A crystalline solid has particles arranged in a regular, repeating three-dimensional pattern.'),
('Class 12 Science', 'Chemistry', 'Solid State, Solutions and Electrochemistry', 'medium', 'mcq',
 'The colligative property that depends on the number of solute particles and is used to determine molar mass is:',
 '[{"text":"Color of the solution","isCorrect":false},{"text":"Depression in freezing point","isCorrect":true},{"text":"Density of the solvent","isCorrect":false},{"text":"Viscosity of the solvent","isCorrect":false}]'::jsonb,
 null, 'Depression in freezing point is a colligative property that depends on the number of solute particles and is used to calculate molar mass.'),
('Class 12 Science', 'Chemistry', 'Solid State, Solutions and Electrochemistry', 'hard', 'mcq',
 'In an electrochemical cell, oxidation occurs at the:',
 '[{"text":"Cathode","isCorrect":false},{"text":"Anode","isCorrect":true},{"text":"Salt bridge","isCorrect":false},{"text":"External circuit only","isCorrect":false}]'::jsonb,
 null, 'In an electrochemical cell, oxidation (loss of electrons) always occurs at the anode.'),
('Class 12 Science', 'Chemistry', 'Solid State, Solutions and Electrochemistry', 'hard', 'mcq',
 'A unit cell in which atoms are present at all 8 corners and the center of the cube is called:',
 '[{"text":"Simple cubic","isCorrect":false},{"text":"Body-centered cubic","isCorrect":true},{"text":"Face-centered cubic","isCorrect":false},{"text":"End-centered cubic","isCorrect":false}]'::jsonb,
 null, 'A body-centered cubic (BCC) unit cell has atoms at all 8 corners plus one atom at the body center.'),

-- Chemistry: Organic Chemistry (Alcohols-Aldehydes-Acids) and Biomolecules
('Class 12 Science', 'Chemistry', 'Organic Chemistry (Alcohols-Aldehydes-Acids) and Biomolecules', 'easy', 'mcq',
 'The functional group -OH is characteristic of which class of organic compounds?',
 '[{"text":"Aldehydes","isCorrect":false},{"text":"Alcohols","isCorrect":true},{"text":"Carboxylic acids","isCorrect":false},{"text":"Ketones","isCorrect":false}]'::jsonb,
 null, 'The hydroxyl group (-OH) is the characteristic functional group of alcohols.'),
('Class 12 Science', 'Chemistry', 'Organic Chemistry (Alcohols-Aldehydes-Acids) and Biomolecules', 'medium', 'mcq',
 'Which of these biomolecules acts as the primary genetic material in most living organisms?',
 '[{"text":"Protein","isCorrect":false},{"text":"DNA","isCorrect":true},{"text":"Carbohydrate","isCorrect":false},{"text":"Lipid","isCorrect":false}]'::jsonb,
 null, 'DNA (deoxyribonucleic acid) carries the genetic information in most living organisms.'),
('Class 12 Science', 'Chemistry', 'Organic Chemistry (Alcohols-Aldehydes-Acids) and Biomolecules', 'hard', 'mcq',
 'The test used to distinguish aldehydes from ketones, based on the reduction of Cu2+ to Cu2O, is called:',
 '[{"text":"Tollens'' test only","isCorrect":false},{"text":"Fehling''s test","isCorrect":true},{"text":"Iodoform test","isCorrect":false},{"text":"Lucas test","isCorrect":false}]'::jsonb,
 null, 'Fehling''s test uses the reduction of Cu2+ (blue) to Cu2O (a brick-red precipitate) to detect aldehydes, which ketones do not give.'),
('Class 12 Science', 'Chemistry', 'Organic Chemistry (Alcohols-Aldehydes-Acids) and Biomolecules', 'hard', 'mcq',
 'The bond that links amino acids together to form a protein chain is called a(n):',
 '[{"text":"Glycosidic bond","isCorrect":false},{"text":"Peptide bond","isCorrect":true},{"text":"Hydrogen bond only","isCorrect":false},{"text":"Ester bond","isCorrect":false}]'::jsonb,
 null, 'Amino acids are linked together by peptide bonds to form protein chains.'),

-- Mathematics: Relations-Functions and Continuity-Differentiability
('Class 12 Science', 'Mathematics', 'Relations-Functions and Continuity-Differentiability', 'easy', 'mcq',
 'A function f is called one-one (injective) if:',
 '[{"text":"Different inputs always give different outputs","isCorrect":true},{"text":"All inputs give the same output","isCorrect":false},{"text":"It has no inverse","isCorrect":false},{"text":"It is not defined for all inputs","isCorrect":false}]'::jsonb,
 null, 'A one-one (injective) function maps distinct inputs to distinct outputs.'),
('Class 12 Science', 'Mathematics', 'Relations-Functions and Continuity-Differentiability', 'medium', 'mcq',
 'What is the derivative of x^3 with respect to x?',
 '[{"text":"x^2","isCorrect":false},{"text":"3x^2","isCorrect":true},{"text":"3x","isCorrect":false},{"text":"x^3/3","isCorrect":false}]'::jsonb,
 null, 'Using the power rule: d/dx(x^n) = nx^(n-1), so d/dx(x^3) = 3x^2.'),
('Class 12 Science', 'Mathematics', 'Relations-Functions and Continuity-Differentiability', 'hard', 'mcq',
 'If y = sin(x^2), what is dy/dx?',
 '[{"text":"cos(x^2)","isCorrect":false},{"text":"2x cos(x^2)","isCorrect":true},{"text":"2x sin(x^2)","isCorrect":false},{"text":"x^2 cos(x^2)","isCorrect":false}]'::jsonb,
 null, 'Using the chain rule: dy/dx = cos(x^2) x d/dx(x^2) = cos(x^2) x 2x = 2x cos(x^2).'),
('Class 12 Science', 'Mathematics', 'Relations-Functions and Continuity-Differentiability', 'hard', 'mcq',
 'A function f(x) is continuous at x=a if:',
 '[{"text":"f(a) is undefined","isCorrect":false},{"text":"The limit of f(x) as x approaches a equals f(a)","isCorrect":true},{"text":"f(x) has a jump at x=a","isCorrect":false},{"text":"f(x) is always increasing","isCorrect":false}]'::jsonb,
 null, 'A function is continuous at x=a if the limit of f(x) as x approaches a exists and equals f(a).'),

-- Mathematics: Integrals, Differential Equations, Vectors and 3D Geometry
('Class 12 Science', 'Mathematics', 'Integrals, Differential Equations, Vectors and 3D Geometry', 'easy', 'mcq',
 'What is the integral of x with respect to x?',
 '[{"text":"x + C","isCorrect":false},{"text":"x^2/2 + C","isCorrect":true},{"text":"x^2 + C","isCorrect":false},{"text":"1 + C","isCorrect":false}]'::jsonb,
 null, 'Using the power rule for integration: integral of x^n dx = x^(n+1)/(n+1) + C, so integral of x dx = x^2/2 + C.'),
('Class 12 Science', 'Mathematics', 'Integrals, Differential Equations, Vectors and 3D Geometry', 'medium', 'mcq',
 'What is the order of the differential equation d^2y/dx^2 + dy/dx = 0?',
 '[{"text":"1","isCorrect":false},{"text":"2","isCorrect":true},{"text":"0","isCorrect":false},{"text":"3","isCorrect":false}]'::jsonb,
 null, 'The order of a differential equation is the order of the highest derivative present, which is 2 here.'),
('Class 12 Science', 'Mathematics', 'Integrals, Differential Equations, Vectors and 3D Geometry', 'hard', 'mcq',
 'What is the magnitude of the vector 3i + 4j?',
 '[{"text":"5","isCorrect":true},{"text":"7","isCorrect":false},{"text":"12","isCorrect":false},{"text":"25","isCorrect":false}]'::jsonb,
 null, 'Magnitude = square root of (3^2 + 4^2) = square root of 25 = 5.'),
('Class 12 Science', 'Mathematics', 'Integrals, Differential Equations, Vectors and 3D Geometry', 'hard', 'mcq',
 'What is the dot product of vectors a = 2i + 3j and b = i - 2j?',
 '[{"text":"-4","isCorrect":true},{"text":"4","isCorrect":false},{"text":"8","isCorrect":false},{"text":"1","isCorrect":false}]'::jsonb,
 null, 'Dot product = (2)(1) + (3)(-2) = 2 - 6 = -4.'),

-- English: Advanced Grammar (Modals, Voice) and Note-Making
('Class 12 Science', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'easy', 'mcq',
 'Choose the correct modal expressing ability in the past: "She ___ swim very well when she was young."',
 '[{"text":"could","isCorrect":true},{"text":"should","isCorrect":false},{"text":"must","isCorrect":false},{"text":"will","isCorrect":false}]'::jsonb,
 null, '''Could'' expresses past ability, correctly showing she had the ability to swim well when young.'),
('Class 12 Science', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'medium', 'mcq',
 'Change into passive voice: "They will announce the results tomorrow."',
 '[{"text":"The results will be announced tomorrow.","isCorrect":true},{"text":"The results are announced tomorrow.","isCorrect":false},{"text":"The results were announced tomorrow.","isCorrect":false},{"text":"The results will announce tomorrow.","isCorrect":false}]'::jsonb,
 null, 'The future active ''will announce'' becomes ''will be announced'' in the passive voice.'),
('Class 12 Science', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'hard', 'mcq',
 'In note-making, the standard abbreviation for "in other words" is:',
 '[{"text":"i.o.w.","isCorrect":true},{"text":"o.w.i.","isCorrect":false},{"text":"n.b.","isCorrect":false},{"text":"cf.","isCorrect":false}]'::jsonb,
 null, '"i.o.w." is a commonly used note-making abbreviation for "in other words".'),
('Class 12 Science', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'hard', 'mcq',
 'Choose the correct modal expressing a strong logical deduction: "The lights are off; they ___ have left already."',
 '[{"text":"must","isCorrect":true},{"text":"might","isCorrect":false},{"text":"can","isCorrect":false},{"text":"shall","isCorrect":false}]'::jsonb,
 null, '''Must have'' expresses a confident logical deduction about a past event based on evidence.'),

-- English: Reading Comprehension and Vocabulary
('Class 12 Science', 'English', 'Reading Comprehension and Vocabulary', 'easy', 'mcq',
 'Read: "The vaccine''s rapid development was hailed as an unprecedented scientific achievement." How was the vaccine''s development described?',
 '[{"text":"As a failure","isCorrect":false},{"text":"As an unprecedented achievement","isCorrect":true},{"text":"As too slow","isCorrect":false},{"text":"As unimportant","isCorrect":false}]'::jsonb,
 null, 'The passage says the vaccine''s rapid development was hailed as an unprecedented scientific achievement.'),
('Class 12 Science', 'English', 'Reading Comprehension and Vocabulary', 'medium', 'mcq',
 'A word that means the same as ''innate'' is:',
 '[{"text":"Learned","isCorrect":false},{"text":"Inborn","isCorrect":true},{"text":"Artificial","isCorrect":false},{"text":"Temporary","isCorrect":false}]'::jsonb,
 null, '''Innate'' means existing from birth, matching ''inborn''.'),
('Class 12 Science', 'English', 'Reading Comprehension and Vocabulary', 'hard', 'mcq',
 'A word that means the opposite of ''ephemeral'' is:',
 '[{"text":"Fleeting","isCorrect":false},{"text":"Enduring","isCorrect":true},{"text":"Brief","isCorrect":false},{"text":"Momentary","isCorrect":false}]'::jsonb,
 null, '''Ephemeral'' means short-lived; its opposite is ''enduring'', meaning lasting a long time.'),
('Class 12 Science', 'English', 'Reading Comprehension and Vocabulary', 'hard', 'mcq',
 'Read: "The researcher''s hypothesis, initially met with skepticism, was later corroborated by independent studies." What eventually happened to the hypothesis?',
 '[{"text":"It was disproven","isCorrect":false},{"text":"It was confirmed by independent studies","isCorrect":true},{"text":"It was forgotten","isCorrect":false},{"text":"It was never tested again","isCorrect":false}]'::jsonb,
 null, '''Corroborated'' means confirmed or supported, so the passage says the hypothesis was later confirmed by independent studies.');
