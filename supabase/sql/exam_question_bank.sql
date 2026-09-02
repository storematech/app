-- supabase/sql/exam_question_bank.sql
--
-- Same idea as exam_patterns.sql, one level deeper: instead of only caching the exam's STRUCTURE
-- (subjects/chapters/weightage), this caches actual hand-written QUESTIONS per exam/subject/
-- chapter/format, so generate-full-test can serve a real question from here before ever spending
-- an AI call on it. generateChapter() now checks this table first for each requested chapter and
-- only asks the AI to fill whatever shortfall the bank doesn't cover — for a chapter fully
-- covered by curated rows, that chapter costs zero AI calls, zero rate-limit exposure, and is
-- never wrong in the way an AI-hallucinated numeric answer occasionally is.
--
-- subject/chapter values MUST exactly match the strings in exam_patterns.subjects[].name /
-- .chapters[].name for the same exam_name — that's what the client echoes back in the Configure
-- step's per-chapter config, and what generateChapter matches against.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql. Safe to re-run (create table
-- if not exists; re-running the inserts below is idempotent since they're keyed by an explicit id).

create table if not exists exam_question_bank (
  id uuid primary key default gen_random_uuid(),
  exam_name text not null,
  subject text not null,
  chapter text not null,
  difficulty text not null check (difficulty in ('easy', 'medium', 'hard')),
  format text not null check (format in ('mcq', 'numerical', 'descriptive')),
  text text not null,
  -- [{ "text": string, "isCorrect": boolean }] — mcq only, null for numerical/descriptive.
  options jsonb,
  -- numerical only — the expected final numeric answer, as a string (e.g. "3", "0.5").
  numerical_answer text,
  -- descriptive: the model answer/explanation to grade against. mcq/numerical: optional extra context.
  explanation text,
  created_at timestamptz not null default now()
);

create index if not exists exam_question_bank_lookup_idx
  on exam_question_bank (lower(exam_name), subject, chapter);

-- No policies on purpose — only ever read/written by generate-full-test via the service-role key,
-- same reasoning as ai_generation_log/exam_patterns.
alter table exam_question_bank enable row level security;

-- ---- Seed data: JEE Main -------------------------------------------------------------------
-- 4 hand-written questions per chapter (2 MCQ + 1 numerical + 1 descriptive), covering every
-- chapter from the exam_patterns.sql JEE Main seed. A starting question bank, not exhaustive —
-- expand over time (either by hand, or by letting cacheAiResearchedPattern's sibling for
-- questions grow it from real AI-generated batches, if that's added later).

insert into exam_question_bank (exam_name, subject, chapter, difficulty, format, text, options, numerical_answer, explanation) values

-- Physics: Mechanics
('JEE Main', 'Physics', 'Mechanics (Kinematics, Laws of Motion, Work-Energy-Power, Rotational Motion, Gravitation)', 'easy', 'mcq',
 'A body starts from rest and moves with constant acceleration, covering 100 m in 10 s. Find its acceleration.',
 '[{"text":"1 m/s^2","isCorrect":false},{"text":"2 m/s^2","isCorrect":true},{"text":"5 m/s^2","isCorrect":false},{"text":"10 m/s^2","isCorrect":false}]'::jsonb,
 null, 'Using s = (1/2)at^2: 100 = 0.5 * a * 100, so a = 2 m/s^2.'),
('JEE Main', 'Physics', 'Mechanics (Kinematics, Laws of Motion, Work-Energy-Power, Rotational Motion, Gravitation)', 'medium', 'mcq',
 'A 2 kg block on a frictionless surface is pulled by a 10 N force acting at 37 degrees above the horizontal (cos37=0.8, sin37=0.6). Find its acceleration.',
 '[{"text":"2 m/s^2","isCorrect":false},{"text":"4 m/s^2","isCorrect":true},{"text":"5 m/s^2","isCorrect":false},{"text":"8 m/s^2","isCorrect":false}]'::jsonb,
 null, 'Horizontal component of force = 10*0.8 = 8 N. a = F/m = 8/2 = 4 m/s^2.'),
('JEE Main', 'Physics', 'Mechanics (Kinematics, Laws of Motion, Work-Energy-Power, Rotational Motion, Gravitation)', 'medium', 'numerical',
 'A stone is dropped from a height of 45 m. Taking g = 10 m/s^2, find the time (in seconds) taken to reach the ground.',
 null, '3', 't = sqrt(2h/g) = sqrt(90/10) = sqrt(9) = 3 s.'),
('JEE Main', 'Physics', 'Mechanics (Kinematics, Laws of Motion, Work-Energy-Power, Rotational Motion, Gravitation)', 'medium', 'descriptive',
 'State the law of conservation of angular momentum and give one real-life example of it.',
 null, null, 'Angular momentum L = I*omega of a system stays constant when no external torque acts on it. Example: a figure skater spins faster when pulling their arms in, since I decreases while L stays constant.'),

-- Physics: Electrodynamics
('JEE Main', 'Physics', 'Electrodynamics (Electrostatics, Current Electricity, Magnetism, EMI, AC)', 'easy', 'mcq',
 'Two point charges +2 uC and -2 uC are placed 1 m apart. Find the magnitude of the electric field at the midpoint (k = 9x10^9 Nm^2/C^2).',
 '[{"text":"7.2x10^4 N/C","isCorrect":false},{"text":"1.44x10^5 N/C","isCorrect":true},{"text":"3.6x10^4 N/C","isCorrect":false},{"text":"2.88x10^5 N/C","isCorrect":false}]'::jsonb,
 null, 'Each charge contributes E = kq/r^2 = 9e9*2e-6/0.25 = 7.2e4 N/C at the midpoint (r=0.5 m); since the charges are opposite, both fields point the same way at the midpoint and add: total = 1.44x10^5 N/C.'),
('JEE Main', 'Physics', 'Electrodynamics (Electrostatics, Current Electricity, Magnetism, EMI, AC)', 'easy', 'mcq',
 'A 12 V battery with internal resistance 1 ohm is connected to an external resistor of 5 ohm. Find the circuit current.',
 '[{"text":"1 A","isCorrect":false},{"text":"2 A","isCorrect":true},{"text":"3 A","isCorrect":false},{"text":"12 A","isCorrect":false}]'::jsonb,
 null, 'I = V/(R+r) = 12/(5+1) = 2 A.'),
('JEE Main', 'Physics', 'Electrodynamics (Electrostatics, Current Electricity, Magnetism, EMI, AC)', 'hard', 'numerical',
 'A wire of resistance 10 ohm is stretched uniformly to twice its original length, with its volume unchanged. Find its new resistance (in ohm).',
 null, '40', 'Since volume V = A*L is constant, R = rho*L/A = rho*L^2/V, so R is proportional to L^2. Doubling L quadruples R: new R = 4*10 = 40 ohm.'),
('JEE Main', 'Physics', 'Electrodynamics (Electrostatics, Current Electricity, Magnetism, EMI, AC)', 'medium', 'descriptive',
 'Explain Lenz''s law and state which fundamental physical principle it follows from.',
 null, null, 'Lenz''s law states the induced EMF/current always opposes the change in magnetic flux causing it. It follows from conservation of energy: if induced current aided the change instead, energy would be created from nothing.'),

-- Physics: Modern Physics
('JEE Main', 'Physics', 'Modern Physics (Atoms, Nuclei, Dual Nature of Matter, Semiconductors)', 'easy', 'mcq',
 'According to Bohr''s model, the radius of the nth orbit of the hydrogen atom is proportional to:',
 '[{"text":"n","isCorrect":false},{"text":"n^2","isCorrect":true},{"text":"1/n","isCorrect":false},{"text":"1/n^2","isCorrect":false}]'::jsonb,
 null, 'Bohr radius r_n = n^2 * (h^2 eps0)/(pi m e^2), so r_n is proportional to n^2.'),
('JEE Main', 'Physics', 'Modern Physics (Atoms, Nuclei, Dual Nature of Matter, Semiconductors)', 'medium', 'mcq',
 'The work function of a metal is 2 eV. Find the threshold wavelength for photoelectric emission (hc = 1240 eV*nm).',
 '[{"text":"310 nm","isCorrect":false},{"text":"465 nm","isCorrect":false},{"text":"620 nm","isCorrect":true},{"text":"1240 nm","isCorrect":false}]'::jsonb,
 null, 'Threshold wavelength = hc/W = 1240/2 = 620 nm.'),
('JEE Main', 'Physics', 'Modern Physics (Atoms, Nuclei, Dual Nature of Matter, Semiconductors)', 'medium', 'numerical',
 'The half-life of a radioactive substance is 20 minutes. What percentage of the original substance remains after 60 minutes?',
 null, '12.5', '60 minutes is 3 half-lives, so the remaining fraction is (1/2)^3 = 1/8 = 12.5%.'),
('JEE Main', 'Physics', 'Modern Physics (Atoms, Nuclei, Dual Nature of Matter, Semiconductors)', 'hard', 'descriptive',
 'Explain the photoelectric effect and why the wave theory of light could not account for it.',
 null, null, 'Light above a threshold frequency ejects electrons from a metal instantly, with the electrons'' maximum kinetic energy depending on frequency (not intensity), and no emission occurs below the threshold frequency regardless of intensity. Wave theory predicted energy depends on intensity, emission at any frequency given enough time, and a time lag at low intensity -- none of which matched experiment. Einstein''s photon model (E=hf) explained it correctly.'),

-- Physics: Heat and Thermodynamics
('JEE Main', 'Physics', 'Heat and Thermodynamics', 'easy', 'mcq',
 'Find the efficiency of a Carnot engine operating between a hot reservoir at 500 K and a cold reservoir at 300 K.',
 '[{"text":"20%","isCorrect":false},{"text":"40%","isCorrect":true},{"text":"60%","isCorrect":false},{"text":"80%","isCorrect":false}]'::jsonb,
 null, 'Efficiency = 1 - Tc/Th = 1 - 300/500 = 0.4 = 40%.'),
('JEE Main', 'Physics', 'Heat and Thermodynamics', 'medium', 'mcq',
 'One mole of an ideal monoatomic gas is heated at constant volume, raising its temperature by 100 K (R = 8.31 J/mol.K, Cv = 1.5R). Find the heat absorbed.',
 '[{"text":"831 J","isCorrect":false},{"text":"1247 J","isCorrect":true},{"text":"2077 J","isCorrect":false},{"text":"415 J","isCorrect":false}]'::jsonb,
 null, 'Q = n*Cv*dT = 1 * 1.5 * 8.31 * 100 = 1246.5 J, approximately 1247 J.'),
('JEE Main', 'Physics', 'Heat and Thermodynamics', 'easy', 'numerical',
 'A gas at pressure 2x10^5 Pa expands isothermally to twice its original volume. Find the final pressure, in units of 10^5 Pa (e.g. answer 1 for 1x10^5 Pa).',
 null, '1', 'Isothermal process: P1V1 = P2V2. Since V2 = 2V1, P2 = P1/2 = 1x10^5 Pa.'),
('JEE Main', 'Physics', 'Heat and Thermodynamics', 'hard', 'descriptive',
 'State the second law of thermodynamics in terms of entropy and explain why a perpetual motion machine of the second kind is impossible.',
 null, null, 'The entropy of an isolated system never decreases -- it increases for any real (irreversible) process. A perpetual motion machine of the second kind would convert heat completely into work with no other effect, decreasing the universe''s entropy, which violates this law.'),

-- Physics: Optics
('JEE Main', 'Physics', 'Optics (Ray and Wave)', 'medium', 'mcq',
 'A convex lens of focal length 20 cm forms a real image 60 cm from the lens. Find the object distance.',
 '[{"text":"15 cm","isCorrect":false},{"text":"30 cm","isCorrect":true},{"text":"40 cm","isCorrect":false},{"text":"60 cm","isCorrect":false}]'::jsonb,
 null, 'Using 1/v - 1/u = 1/f with v=+60, f=+20: 1/u = 1/60 - 1/20 = -1/30, so u = -30 cm; object distance magnitude is 30 cm.'),
('JEE Main', 'Physics', 'Optics (Ray and Wave)', 'medium', 'mcq',
 'In Young''s double slit experiment, the fringe width is 0.3 mm for light of wavelength 600 nm. Find the new fringe width if the wavelength is changed to 400 nm (everything else unchanged).',
 '[{"text":"0.1 mm","isCorrect":false},{"text":"0.2 mm","isCorrect":true},{"text":"0.3 mm","isCorrect":false},{"text":"0.45 mm","isCorrect":false}]'::jsonb,
 null, 'Fringe width is proportional to wavelength: new width = 0.3 * (400/600) = 0.2 mm.'),
('JEE Main', 'Physics', 'Optics (Ray and Wave)', 'easy', 'numerical',
 'Light travels from air (n=1) into glass (n=1.5). If its speed in air is 3x10^8 m/s, find its speed in glass, in units of 10^8 m/s.',
 null, '2', 'v = c/n = 3e8/1.5 = 2x10^8 m/s.'),
('JEE Main', 'Physics', 'Optics (Ray and Wave)', 'medium', 'descriptive',
 'Explain total internal reflection and state the conditions necessary for it to occur.',
 null, null, 'Total internal reflection occurs when light travelling from a denser to a rarer medium strikes the boundary at an angle of incidence greater than the critical angle, so all the light reflects back into the denser medium instead of refracting out. Conditions: light must go from denser to rarer medium, and the angle of incidence must exceed the critical angle.'),

-- Physics: Waves and Oscillations
('JEE Main', 'Physics', 'Waves and Oscillations', 'medium', 'mcq',
 'A simple pendulum has a time period of 2 s on Earth. Find its new time period at a location where g is one-fourth of Earth''s g.',
 '[{"text":"1 s","isCorrect":false},{"text":"2 s","isCorrect":false},{"text":"4 s","isCorrect":true},{"text":"8 s","isCorrect":false}]'::jsonb,
 null, 'T is proportional to 1/sqrt(g). If g becomes g/4, T becomes T*sqrt(4) = 2*2 = 4 s.'),
('JEE Main', 'Physics', 'Waves and Oscillations', 'easy', 'mcq',
 'Two sound waves of frequency 256 Hz and 260 Hz are superposed. Find the beat frequency heard.',
 '[{"text":"2 Hz","isCorrect":false},{"text":"4 Hz","isCorrect":true},{"text":"258 Hz","isCorrect":false},{"text":"516 Hz","isCorrect":false}]'::jsonb,
 null, 'Beat frequency = |f1 - f2| = |260-256| = 4 Hz.'),
('JEE Main', 'Physics', 'Waves and Oscillations', 'easy', 'numerical',
 'A wave of wavelength 2 m travels with speed 340 m/s. Find its frequency, in Hz (nearest whole number).',
 null, '170', 'f = v/lambda = 340/2 = 170 Hz.'),
('JEE Main', 'Physics', 'Waves and Oscillations', 'easy', 'descriptive',
 'Distinguish between transverse and longitudinal waves, giving one example of each.',
 null, null, 'In a transverse wave, particle oscillation is perpendicular to the direction of propagation (e.g. waves on a string, light waves). In a longitudinal wave, particle oscillation is parallel to the direction of propagation (e.g. sound waves in air).'),

-- Physics: Properties of Matter and Fluids
('JEE Main', 'Physics', 'Properties of Matter and Fluids', 'easy', 'mcq',
 'What is the SI unit of surface tension?',
 '[{"text":"N/m","isCorrect":true},{"text":"N/m^2","isCorrect":false},{"text":"N.m","isCorrect":false},{"text":"N.s","isCorrect":false}]'::jsonb,
 null, 'Surface tension is force per unit length, so its SI unit is N/m.'),
('JEE Main', 'Physics', 'Properties of Matter and Fluids', 'medium', 'mcq',
 'A steel ball dropped into a viscous liquid reaches terminal velocity. Which statement about terminal velocity is correct?',
 '[{"text":"Acceleration is maximum","isCorrect":false},{"text":"Net force on the ball is zero","isCorrect":true},{"text":"Velocity is still increasing","isCorrect":false},{"text":"Viscous force is zero","isCorrect":false}]'::jsonb,
 null, 'At terminal velocity, weight is exactly balanced by buoyant force plus viscous drag, so the net force (and acceleration) is zero.'),
('JEE Main', 'Physics', 'Properties of Matter and Fluids', 'hard', 'numerical',
 'A capillary tube of radius 0.5 mm is dipped in water (surface tension 7x10^-2 N/m, density 1000 kg/m^3, g=10 m/s^2, angle of contact 0). Find the height of water rise, in mm (h = 2T/(rho*g*r)).',
 null, '28', 'h = 2*0.07/(1000*10*0.0005) = 0.14/5 = 0.028 m = 28 mm.'),
('JEE Main', 'Physics', 'Properties of Matter and Fluids', 'medium', 'descriptive',
 'Explain what is meant by the elastic limit of a material and what happens if it is stressed beyond it.',
 null, null, 'The elastic limit is the maximum stress a material can bear while still returning to its original shape once the stress is removed. Beyond it, the material undergoes permanent (plastic) deformation and does not fully recover its original shape.'),

-- Chemistry: Physical Chemistry
('JEE Main', 'Chemistry', 'Physical Chemistry (Mole Concept, Thermodynamics, Equilibrium, Electrochemistry, Chemical Kinetics, Solutions)', 'easy', 'mcq',
 'How many moles are present in 22 g of CO2 (molar mass 44 g/mol)?',
 '[{"text":"0.25 mol","isCorrect":false},{"text":"0.5 mol","isCorrect":true},{"text":"1 mol","isCorrect":false},{"text":"2 mol","isCorrect":false}]'::jsonb,
 null, 'n = mass/molar mass = 22/44 = 0.5 mol.'),
('JEE Main', 'Chemistry', 'Physical Chemistry (Mole Concept, Thermodynamics, Equilibrium, Electrochemistry, Chemical Kinetics, Solutions)', 'medium', 'mcq',
 'A first-order reaction has rate constant 0.0231 per minute. Find its half-life (ln2 ~ 0.693).',
 '[{"text":"15 min","isCorrect":false},{"text":"30 min","isCorrect":true},{"text":"45 min","isCorrect":false},{"text":"60 min","isCorrect":false}]'::jsonb,
 null, 't(1/2) = 0.693/k = 0.693/0.0231 = 30 min.'),
('JEE Main', 'Chemistry', 'Physical Chemistry (Mole Concept, Thermodynamics, Equilibrium, Electrochemistry, Chemical Kinetics, Solutions)', 'easy', 'numerical',
 'Find the pH of a 0.01 M HCl solution (a strong acid, fully dissociated).',
 null, '2', '[H+] = 0.01 = 10^-2 M, so pH = 2.'),
('JEE Main', 'Chemistry', 'Physical Chemistry (Mole Concept, Thermodynamics, Equilibrium, Electrochemistry, Chemical Kinetics, Solutions)', 'medium', 'descriptive',
 'State Le Chatelier''s principle and explain how increasing pressure affects a gaseous equilibrium with fewer moles on the product side.',
 null, null, 'Le Chatelier''s principle: if a system at equilibrium is disturbed (concentration, temperature, volume/pressure), it shifts to counteract the change. Increasing pressure (reducing volume) shifts equilibrium toward the side with fewer gas moles, partially relieving the pressure increase.'),

-- Chemistry: Organic Chemistry
('JEE Main', 'Chemistry', 'Organic Chemistry (GOC, Hydrocarbons, Alcohols-Phenols-Ethers, Carbonyl Compounds, Amines, Biomolecules, Polymers)', 'easy', 'mcq',
 'What is the IUPAC name of CH3-CH2-OH?',
 '[{"text":"Methanol","isCorrect":false},{"text":"Ethanol","isCorrect":true},{"text":"Propanol","isCorrect":false},{"text":"Ethanal","isCorrect":false}]'::jsonb,
 null, 'CH3-CH2-OH is a two-carbon alcohol, named ethanol.'),
('JEE Main', 'Chemistry', 'Organic Chemistry (GOC, Hydrocarbons, Alcohols-Phenols-Ethers, Carbonyl Compounds, Amines, Biomolecules, Polymers)', 'medium', 'mcq',
 'Which of these is the strongest acid?',
 '[{"text":"CH3COOH","isCorrect":false},{"text":"ClCH2COOH","isCorrect":false},{"text":"Cl2CHCOOH","isCorrect":false},{"text":"Cl3CCOOH","isCorrect":true}]'::jsonb,
 null, 'More electronegative chlorine substituents pull electron density away (-I effect), stabilizing the conjugate base -- so acidity increases with more Cl atoms: Cl3CCOOH (trichloroacetic acid) is the strongest here.'),
('JEE Main', 'Chemistry', 'Organic Chemistry (GOC, Hydrocarbons, Alcohols-Phenols-Ethers, Carbonyl Compounds, Amines, Biomolecules, Polymers)', 'easy', 'numerical',
 'How many structural isomers does C4H10 (butane) have?',
 null, '2', 'C4H10 has exactly two structural isomers: n-butane and isobutane (2-methylpropane).'),
('JEE Main', 'Chemistry', 'Organic Chemistry (GOC, Hydrocarbons, Alcohols-Phenols-Ethers, Carbonyl Compounds, Amines, Biomolecules, Polymers)', 'medium', 'descriptive',
 'Explain Markovnikov''s rule and state the product formed when HBr adds to propene.',
 null, null, 'Markovnikov''s rule: when HX adds to an unsymmetrical alkene, H attaches to the carbon already bearing more hydrogens, and X attaches to the more substituted carbon (which better stabilizes the intermediate carbocation). For propene + HBr, the product is 2-bromopropane.'),

-- Chemistry: Inorganic Chemistry
('JEE Main', 'Chemistry', 'Inorganic Chemistry (Periodic Table, Chemical Bonding, Coordination Compounds, p/d/f-Block Elements, Metallurgy)', 'easy', 'mcq',
 'What is the electronic configuration of Na+ (Z=11)?',
 '[{"text":"1s2 2s2 2p6","isCorrect":true},{"text":"1s2 2s2 2p6 3s1","isCorrect":false},{"text":"1s2 2s2 2p5","isCorrect":false},{"text":"1s2 2s2 2p6 3s2","isCorrect":false}]'::jsonb,
 null, 'Na (Z=11) loses one electron to form Na+, leaving the neon-like configuration 1s2 2s2 2p6.'),
('JEE Main', 'Chemistry', 'Inorganic Chemistry (Periodic Table, Chemical Bonding, Coordination Compounds, p/d/f-Block Elements, Metallurgy)', 'hard', 'mcq',
 'Which of the following has the maximum lattice energy?',
 '[{"text":"NaCl","isCorrect":false},{"text":"MgO","isCorrect":true},{"text":"KCl","isCorrect":false},{"text":"CaCl2","isCorrect":false}]'::jsonb,
 null, 'MgO has small, doubly-charged ions (Mg2+ and O2-), giving it a much higher lattice energy than the singly-charged alkali halides.'),
('JEE Main', 'Chemistry', 'Inorganic Chemistry (Periodic Table, Chemical Bonding, Coordination Compounds, p/d/f-Block Elements, Metallurgy)', 'medium', 'numerical',
 'Find the oxidation state of Mn in KMnO4 (give just the number, e.g. 7 for +7).',
 null, '7', 'K(+1) + Mn(x) + 4*O(-2) = 0, so x = +7.'),
('JEE Main', 'Chemistry', 'Inorganic Chemistry (Periodic Table, Chemical Bonding, Coordination Compounds, p/d/f-Block Elements, Metallurgy)', 'hard', 'descriptive',
 'Explain why transition metals typically show variable oxidation states, unlike most main-group elements.',
 null, null, 'Transition metals have (n-1)d and ns orbitals close in energy, so electrons from either can be involved in bonding at similar energy cost, making several oxidation states accessible. Main-group elements have a larger energy gap to the next orbital, so they show fewer, more fixed oxidation states.'),

-- Mathematics: Algebra
('JEE Main', 'Mathematics', 'Algebra (Complex Numbers, Quadratic Equations, Sequences and Series, Permutations-Combinations, Binomial Theorem, Matrices and Determinants)', 'easy', 'mcq',
 'Find the sum of the first 10 terms of the arithmetic progression 2, 4, 6, 8, ...',
 '[{"text":"100","isCorrect":false},{"text":"110","isCorrect":true},{"text":"120","isCorrect":false},{"text":"90","isCorrect":false}]'::jsonb,
 null, 'Sum = (n/2)*(2a+(n-1)d) = 5*(4+18) = 110.'),
('JEE Main', 'Mathematics', 'Algebra (Complex Numbers, Quadratic Equations, Sequences and Series, Permutations-Combinations, Binomial Theorem, Matrices and Determinants)', 'medium', 'mcq',
 'In how many ways can 3 objects be chosen from 7 distinct objects?',
 '[{"text":"21","isCorrect":false},{"text":"35","isCorrect":true},{"text":"42","isCorrect":false},{"text":"210","isCorrect":false}]'::jsonb,
 null, 'C(7,3) = 7!/(3!4!) = 35.'),
('JEE Main', 'Mathematics', 'Algebra (Complex Numbers, Quadratic Equations, Sequences and Series, Permutations-Combinations, Binomial Theorem, Matrices and Determinants)', 'easy', 'numerical',
 'If z = 3 + 4i, find the modulus |z|.',
 null, '5', '|z| = sqrt(3^2+4^2) = sqrt(25) = 5.'),
('JEE Main', 'Mathematics', 'Algebra (Complex Numbers, Quadratic Equations, Sequences and Series, Permutations-Combinations, Binomial Theorem, Matrices and Determinants)', 'medium', 'descriptive',
 'State the Binomial theorem for (a+b)^n and write its general term T(r+1).',
 null, null, '(a+b)^n = sum over r=0..n of C(n,r) a^(n-r) b^r. The general term is T(r+1) = C(n,r) * a^(n-r) * b^r.'),

-- Mathematics: Calculus
('JEE Main', 'Mathematics', 'Calculus (Limits-Continuity-Differentiability, Application of Derivatives, Integration, Differential Equations)', 'easy', 'mcq',
 'Find the derivative of x^3 with respect to x.',
 '[{"text":"x^2","isCorrect":false},{"text":"2x^2","isCorrect":false},{"text":"3x^2","isCorrect":true},{"text":"3x^3","isCorrect":false}]'::jsonb,
 null, 'd/dx(x^3) = 3x^2.'),
('JEE Main', 'Mathematics', 'Calculus (Limits-Continuity-Differentiability, Application of Derivatives, Integration, Differential Equations)', 'medium', 'mcq',
 'Evaluate the definite integral of x^2 dx from 0 to 1.',
 '[{"text":"1/2","isCorrect":false},{"text":"1/3","isCorrect":true},{"text":"1/4","isCorrect":false},{"text":"1","isCorrect":false}]'::jsonb,
 null, 'Integral of x^2 is x^3/3; evaluated from 0 to 1 gives 1/3.'),
('JEE Main', 'Mathematics', 'Calculus (Limits-Continuity-Differentiability, Application of Derivatives, Integration, Differential Equations)', 'easy', 'numerical',
 'Find the value of the limit as x approaches 0 of (sin x)/x.',
 null, '1', 'This is the standard limit lim(x->0) sin(x)/x = 1.'),
('JEE Main', 'Mathematics', 'Calculus (Limits-Continuity-Differentiability, Application of Derivatives, Integration, Differential Equations)', 'medium', 'descriptive',
 'State the first derivative test for locating local maxima and minima of a function.',
 null, null, 'If f''(x) changes sign from positive to negative at x=c, f has a local maximum there. If it changes from negative to positive, f has a local minimum there. If the sign does not change, x=c is neither.'),

-- Mathematics: Coordinate Geometry
('JEE Main', 'Mathematics', 'Coordinate Geometry (Straight Lines, Circles, Conic Sections)', 'easy', 'mcq',
 'Find the distance between the points (3,4) and (0,0).',
 '[{"text":"4","isCorrect":false},{"text":"5","isCorrect":true},{"text":"6","isCorrect":false},{"text":"7","isCorrect":false}]'::jsonb,
 null, 'Distance = sqrt(3^2+4^2) = sqrt(25) = 5.'),
('JEE Main', 'Mathematics', 'Coordinate Geometry (Straight Lines, Circles, Conic Sections)', 'easy', 'mcq',
 'Find the equation of a circle with center (2,3) and radius 5.',
 '[{"text":"(x-2)^2+(y-3)^2=5","isCorrect":false},{"text":"(x-2)^2+(y-3)^2=25","isCorrect":true},{"text":"(x+2)^2+(y+3)^2=25","isCorrect":false},{"text":"x^2+y^2=25","isCorrect":false}]'::jsonb,
 null, 'A circle with center (h,k) and radius r has equation (x-h)^2+(y-k)^2=r^2, giving (x-2)^2+(y-3)^2=25.'),
('JEE Main', 'Mathematics', 'Coordinate Geometry (Straight Lines, Circles, Conic Sections)', 'medium', 'numerical',
 'Find the slope of the line through points (1,2) and (3,8).',
 null, '3', 'Slope = (8-2)/(3-1) = 6/2 = 3.'),
('JEE Main', 'Mathematics', 'Coordinate Geometry (Straight Lines, Circles, Conic Sections)', 'medium', 'descriptive',
 'State the condition for two lines with slopes m1 and m2 to be perpendicular.',
 null, null, 'Two lines (neither vertical) are perpendicular if and only if the product of their slopes equals -1, i.e. m1*m2 = -1.'),

-- Mathematics: Trigonometry
('JEE Main', 'Mathematics', 'Trigonometry', 'easy', 'mcq',
 'Find the value of sin(30 degrees).',
 '[{"text":"1/2","isCorrect":true},{"text":"sqrt(3)/2","isCorrect":false},{"text":"1","isCorrect":false},{"text":"0","isCorrect":false}]'::jsonb,
 null, 'sin(30 degrees) = 1/2, a standard trigonometric value.'),
('JEE Main', 'Mathematics', 'Trigonometry', 'medium', 'mcq',
 'If tan(theta) = 3/4 and theta lies in the first quadrant, find sin(theta).',
 '[{"text":"3/5","isCorrect":true},{"text":"4/5","isCorrect":false},{"text":"3/4","isCorrect":false},{"text":"4/3","isCorrect":false}]'::jsonb,
 null, 'Using a 3-4-5 right triangle (opposite=3, adjacent=4, hypotenuse=5), sin(theta) = 3/5.'),
('JEE Main', 'Mathematics', 'Trigonometry', 'easy', 'numerical',
 'Find the value of cos(60 degrees) + sin(30 degrees), as a decimal.',
 null, '1', 'cos(60)=0.5 and sin(30)=0.5, so their sum is 1.'),
('JEE Main', 'Mathematics', 'Trigonometry', 'medium', 'descriptive',
 'State the addition formula for sin(A+B) and use it to derive the formula for sin(2A).',
 null, null, 'sin(A+B) = sinA*cosB + cosA*sinB. Setting B=A: sin(2A) = sinA*cosA + cosA*sinA = 2*sinA*cosA.'),

-- Mathematics: Vectors and 3D Geometry
('JEE Main', 'Mathematics', 'Vectors and 3D Geometry', 'easy', 'mcq',
 'Find the magnitude of the vector a = 3i + 4j.',
 '[{"text":"5","isCorrect":true},{"text":"7","isCorrect":false},{"text":"12","isCorrect":false},{"text":"25","isCorrect":false}]'::jsonb,
 null, '|a| = sqrt(3^2+4^2) = sqrt(25) = 5.'),
('JEE Main', 'Mathematics', 'Vectors and 3D Geometry', 'medium', 'mcq',
 'Find the dot product of a=(1,2,3) and b=(4,-5,6).',
 '[{"text":"4","isCorrect":false},{"text":"12","isCorrect":true},{"text":"20","isCorrect":false},{"text":"-12","isCorrect":false}]'::jsonb,
 null, 'a.b = 1*4 + 2*(-5) + 3*6 = 4 - 10 + 18 = 12.'),
('JEE Main', 'Mathematics', 'Vectors and 3D Geometry', 'medium', 'numerical',
 'If a and b are perpendicular unit vectors, find |a x b|.',
 null, '1', '|a x b| = |a||b|sin(theta) = 1*1*sin(90) = 1.'),
('JEE Main', 'Mathematics', 'Vectors and 3D Geometry', 'medium', 'descriptive',
 'Explain the geometrical significance of the cross product of two vectors.',
 null, null, 'The cross product a x b is a vector perpendicular to both a and b (by the right-hand rule), whose magnitude |a||b|sin(theta) equals the area of the parallelogram formed by a and b.'),

-- Mathematics: Statistics and Probability
('JEE Main', 'Mathematics', 'Statistics and Probability', 'easy', 'mcq',
 'A fair coin is tossed twice. Find the probability of getting exactly one head.',
 '[{"text":"1/4","isCorrect":false},{"text":"1/2","isCorrect":true},{"text":"3/4","isCorrect":false},{"text":"1","isCorrect":false}]'::jsonb,
 null, 'Outcomes: HH, HT, TH, TT. Exactly one head occurs in HT and TH, giving probability 2/4 = 1/2.'),
('JEE Main', 'Mathematics', 'Statistics and Probability', 'easy', 'mcq',
 'Find the mean of the data set 2, 4, 6, 8, 10.',
 '[{"text":"5","isCorrect":false},{"text":"6","isCorrect":true},{"text":"7","isCorrect":false},{"text":"8","isCorrect":false}]'::jsonb,
 null, 'Mean = (2+4+6+8+10)/5 = 30/5 = 6.'),
('JEE Main', 'Mathematics', 'Statistics and Probability', 'easy', 'numerical',
 'A fair six-sided die is rolled once. Find the probability of getting an even number, as a decimal.',
 null, '0.5', 'Even outcomes are 2, 4, 6 out of 6 total, giving probability 3/6 = 0.5.'),
('JEE Main', 'Mathematics', 'Statistics and Probability', 'medium', 'descriptive',
 'Define the variance of a data set and state the difference between variance and standard deviation.',
 null, null, 'Variance is the average of the squared deviations of each data point from the mean. Standard deviation is the square root of variance, bringing the spread measure back to the same units as the original data.');
