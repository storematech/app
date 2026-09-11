-- supabase/sql/exam_question_bank_class12_commerce_set1.sql
--
-- First curated question-bank batch for 'Class 12 Commerce' (see
-- exam_patterns_class12_commerce.sql for the exam's structure). Same exam_question_bank
-- table/schema as the other sets -- no migration needed.
--
-- Every row here is format='mcq' with a single isCorrect:true option, matching this exam's
-- MCQ-only, no-negative-marking pattern. 8 chapters (2 per subject x 4 subjects: Accountancy,
-- Business Studies, Economics, English) each get 4 questions -- one easy, one medium, two hard --
-- reflecting the final-board-year hard share in exam_patterns_class12_commerce.sql. Pitched one
-- notch above Class 11 Commerce (see exam_question_bank_class11_commerce_set1.sql).
--
-- Run this once in the Supabase SQL Editor, after exam_patterns_class12_commerce.sql.

insert into exam_question_bank (exam_name, subject, chapter, difficulty, format, text, options, numerical_answer, explanation) values

-- Accountancy: Partnership Accounts (Reconstitution and Dissolution)
('Class 12 Commerce', 'Accountancy', 'Partnership Accounts (Reconstitution and Dissolution)', 'easy', 'mcq',
 'When a new partner is admitted into a firm, the process is called:',
 '[{"text":"Dissolution of partnership","isCorrect":false},{"text":"Reconstitution of partnership","isCorrect":true},{"text":"Liquidation","isCorrect":false},{"text":"Amalgamation only","isCorrect":false}]'::jsonb,
 null, 'Admission of a new partner changes the existing agreement, making it a reconstitution of the partnership.'),
('Class 12 Commerce', 'Accountancy', 'Partnership Accounts (Reconstitution and Dissolution)', 'medium', 'mcq',
 'The ratio in which the remaining partners acquire the retiring partner''s share of profit is called the:',
 '[{"text":"Sacrificing ratio","isCorrect":false},{"text":"Gaining ratio","isCorrect":true},{"text":"Capital ratio","isCorrect":false},{"text":"Liquidity ratio","isCorrect":false}]'::jsonb,
 null, 'The gaining ratio is the ratio in which remaining partners acquire the retiring or deceased partner''s share of profit.'),
('Class 12 Commerce', 'Accountancy', 'Partnership Accounts (Reconstitution and Dissolution)', 'hard', 'mcq',
 'On the dissolution of a firm, the account prepared to close all asset and liability accounts and determine profit or loss on realization is the:',
 '[{"text":"Revaluation account","isCorrect":false},{"text":"Realization account","isCorrect":true},{"text":"Partners'' capital account","isCorrect":false},{"text":"Profit and loss appropriation account","isCorrect":false}]'::jsonb,
 null, 'The realization account is prepared on dissolution to record the sale of assets and settlement of liabilities, and to determine the profit or loss on realization.'),
('Class 12 Commerce', 'Accountancy', 'Partnership Accounts (Reconstitution and Dissolution)', 'hard', 'mcq',
 'Goodwill raised at the time of a partner''s retirement and not written off is normally adjusted through the capital accounts of:',
 '[{"text":"The retiring partner only","isCorrect":false},{"text":"All partners in their old profit-sharing ratio, then written back in new ratio","isCorrect":true},{"text":"The new partner only","isCorrect":false},{"text":"No one -- it stays permanently on the books","isCorrect":false}]'::jsonb,
 null, 'Goodwill is typically raised in the old ratio and written off in the new profit-sharing ratio among the continuing partners.'),

-- Accountancy: Company Accounts (Shares-Debentures) and Financial Statement Analysis
('Class 12 Commerce', 'Accountancy', 'Company Accounts (Shares-Debentures) and Financial Statement Analysis', 'easy', 'mcq',
 'A fixed-income security issued by a company as a form of long-term borrowing is called a:',
 '[{"text":"Share","isCorrect":false},{"text":"Debenture","isCorrect":true},{"text":"Dividend","isCorrect":false},{"text":"Bonus issue","isCorrect":false}]'::jsonb,
 null, 'A debenture is a debt instrument used by companies to raise long-term borrowed funds, carrying a fixed rate of interest.'),
('Class 12 Commerce', 'Accountancy', 'Company Accounts (Shares-Debentures) and Financial Statement Analysis', 'medium', 'mcq',
 'When shares are issued at a price higher than their face value, the excess amount is credited to the:',
 '[{"text":"Securities premium account","isCorrect":true},{"text":"Discount on issue account","isCorrect":false},{"text":"General reserve only","isCorrect":false},{"text":"Capital redemption reserve","isCorrect":false}]'::jsonb,
 null, 'The excess received over the face value of shares is credited to the securities premium account.'),
('Class 12 Commerce', 'Accountancy', 'Company Accounts (Shares-Debentures) and Financial Statement Analysis', 'hard', 'mcq',
 'The ratio that measures a company''s ability to meet its short-term obligations using its most liquid assets is the:',
 '[{"text":"Debt-equity ratio","isCorrect":false},{"text":"Quick (acid-test) ratio","isCorrect":true},{"text":"Gross profit ratio","isCorrect":false},{"text":"Return on investment","isCorrect":false}]'::jsonb,
 null, 'The quick (acid-test) ratio measures short-term liquidity using only the most liquid current assets (excluding inventory).'),
('Class 12 Commerce', 'Accountancy', 'Company Accounts (Shares-Debentures) and Financial Statement Analysis', 'hard', 'mcq',
 'Under the Companies Act, forfeited shares can be reissued at a discount, but the total discount allowed cannot exceed the:',
 '[{"text":"Amount forfeited on those shares","isCorrect":true},{"text":"Face value of new shares only","isCorrect":false},{"text":"Company''s total reserves","isCorrect":false},{"text":"Market value of the shares","isCorrect":false}]'::jsonb,
 null, 'The discount allowed on reissue of forfeited shares cannot exceed the amount that was forfeited on those shares.'),

-- Business Studies: Principles and Functions of Management
('Class 12 Commerce', 'Business Studies', 'Principles and Functions of Management', 'easy', 'mcq',
 'Which of these is one of the basic functions of management?',
 '[{"text":"Planning","isCorrect":true},{"text":"Advertising only","isCorrect":false},{"text":"Manufacturing only","isCorrect":false},{"text":"Selling only","isCorrect":false}]'::jsonb,
 null, 'Planning is one of the basic functions of management, alongside organizing, staffing, directing, and controlling.'),
('Class 12 Commerce', 'Business Studies', 'Principles and Functions of Management', 'medium', 'mcq',
 'Fayol''s principle stating that there should be one plan for a group of activities with the same objective is called:',
 '[{"text":"Unity of command","isCorrect":false},{"text":"Unity of direction","isCorrect":true},{"text":"Division of work","isCorrect":false},{"text":"Espirit de corps","isCorrect":false}]'::jsonb,
 null, 'Unity of direction means one plan and one head should guide a group of activities aimed at the same objective.'),
('Class 12 Commerce', 'Business Studies', 'Principles and Functions of Management', 'hard', 'mcq',
 'The management function concerned with grouping activities, assigning duties, and establishing authority relationships is called:',
 '[{"text":"Organizing","isCorrect":true},{"text":"Staffing","isCorrect":false},{"text":"Directing","isCorrect":false},{"text":"Controlling","isCorrect":false}]'::jsonb,
 null, 'Organizing involves grouping activities, assigning duties, and establishing authority relationships to achieve organizational goals.'),
('Class 12 Commerce', 'Business Studies', 'Principles and Functions of Management', 'hard', 'mcq',
 'Taylor''s scientific management technique that involves timing each element of a task to determine the best way to perform it is called:',
 '[{"text":"Time study","isCorrect":true},{"text":"Fatigue study","isCorrect":false},{"text":"Simplification","isCorrect":false},{"text":"Standardization","isCorrect":false}]'::jsonb,
 null, 'Time study determines the standard time needed to perform a task, a key technique in Taylor''s scientific management.'),

-- Business Studies: Marketing, Consumer Protection and Entrepreneurship
('Class 12 Commerce', 'Business Studies', 'Marketing, Consumer Protection and Entrepreneurship', 'easy', 'mcq',
 'The combination of product, price, place, and promotion used by a business is called the:',
 '[{"text":"Marketing mix","isCorrect":true},{"text":"Business cycle","isCorrect":false},{"text":"Supply chain","isCorrect":false},{"text":"Value chain","isCorrect":false}]'::jsonb,
 null, 'The marketing mix consists of the four Ps -- product, price, place, and promotion.'),
('Class 12 Commerce', 'Business Studies', 'Marketing, Consumer Protection and Entrepreneurship', 'medium', 'mcq',
 'A consumer''s right to be informed about the quality, quantity, and price of goods is known as the:',
 '[{"text":"Right to safety","isCorrect":false},{"text":"Right to be informed","isCorrect":true},{"text":"Right to choose","isCorrect":false},{"text":"Right to be heard","isCorrect":false}]'::jsonb,
 null, 'The right to be informed ensures consumers have access to accurate information about a product''s quality, quantity, and price.'),
('Class 12 Commerce', 'Business Studies', 'Marketing, Consumer Protection and Entrepreneurship', 'hard', 'mcq',
 'A new venture that is scalable, innovative, and often technology-driven, typically seeking external funding to grow rapidly, is best described as a:',
 '[{"text":"Sole proprietorship only","isCorrect":false},{"text":"Startup","isCorrect":true},{"text":"Government enterprise","isCorrect":false},{"text":"Cooperative society","isCorrect":false}]'::jsonb,
 null, 'A startup is typically a scalable, innovative new venture, often seeking external funding to grow rapidly.'),
('Class 12 Commerce', 'Business Studies', 'Marketing, Consumer Protection and Entrepreneurship', 'hard', 'mcq',
 'The stage in a product''s life cycle where sales peak and competition is most intense is the:',
 '[{"text":"Introduction stage","isCorrect":false},{"text":"Maturity stage","isCorrect":true},{"text":"Decline stage","isCorrect":false},{"text":"Growth stage only","isCorrect":false}]'::jsonb,
 null, 'In the maturity stage of the product life cycle, sales peak and competition among firms is typically at its most intense.'),

-- Economics: Macroeconomics: National Income and Money-Banking
('Class 12 Commerce', 'Economics', 'Macroeconomics: National Income and Money-Banking', 'easy', 'mcq',
 'The total value of all final goods and services produced within a country in a year is called its:',
 '[{"text":"Gross Domestic Product (GDP)","isCorrect":true},{"text":"Per capita income","isCorrect":false},{"text":"Fiscal deficit","isCorrect":false},{"text":"Trade balance","isCorrect":false}]'::jsonb,
 null, 'GDP measures the total value of all final goods and services produced within a country''s borders in a given year.'),
('Class 12 Commerce', 'Economics', 'Macroeconomics: National Income and Money-Banking', 'medium', 'mcq',
 'Which of these is the primary function of a central bank like the RBI?',
 '[{"text":"Issuing currency and regulating money supply","isCorrect":true},{"text":"Selling consumer goods","isCorrect":false},{"text":"Manufacturing products","isCorrect":false},{"text":"Running retail stores","isCorrect":false}]'::jsonb,
 null, 'A central bank''s primary functions include issuing currency and regulating the money supply in the economy.'),
('Class 12 Commerce', 'Economics', 'Macroeconomics: National Income and Money-Banking', 'hard', 'mcq',
 'Nominal GDP differs from real GDP because nominal GDP is measured:',
 '[{"text":"At constant prices","isCorrect":false},{"text":"At current market prices, without adjusting for inflation","isCorrect":true},{"text":"Only for exports","isCorrect":false},{"text":"Excluding government spending","isCorrect":false}]'::jsonb,
 null, 'Nominal GDP is measured at current market prices and is not adjusted for inflation, unlike real GDP.'),
('Class 12 Commerce', 'Economics', 'Macroeconomics: National Income and Money-Banking', 'hard', 'mcq',
 'The Cash Reserve Ratio (CRR) is a tool used by the central bank to control:',
 '[{"text":"Foreign exchange rates only","isCorrect":false},{"text":"The money supply in the economy","isCorrect":true},{"text":"Stock market prices directly","isCorrect":false},{"text":"Income tax rates","isCorrect":false}]'::jsonb,
 null, 'CRR is the percentage of deposits banks must keep with the central bank, used as a monetary policy tool to control money supply.'),

-- Economics: Government Budget, Balance of Payments and Indian Economic Development
('Class 12 Commerce', 'Economics', 'Government Budget, Balance of Payments and Indian Economic Development', 'easy', 'mcq',
 'A government budget in which total expenditure exceeds total revenue is called a:',
 '[{"text":"Balanced budget","isCorrect":false},{"text":"Deficit budget","isCorrect":true},{"text":"Surplus budget","isCorrect":false},{"text":"Zero-based budget","isCorrect":false}]'::jsonb,
 null, 'A deficit budget occurs when the government''s total expenditure exceeds its total revenue.'),
('Class 12 Commerce', 'Economics', 'Government Budget, Balance of Payments and Indian Economic Development', 'medium', 'mcq',
 'A record of all economic transactions of a country with the rest of the world during a given period is called the:',
 '[{"text":"Balance of payments","isCorrect":true},{"text":"Union budget","isCorrect":false},{"text":"Fiscal policy","isCorrect":false},{"text":"National income","isCorrect":false}]'::jsonb,
 null, 'The balance of payments records all economic transactions between a country and the rest of the world over a given period.'),
('Class 12 Commerce', 'Economics', 'Government Budget, Balance of Payments and Indian Economic Development', 'hard', 'mcq',
 'India''s economic reforms of 1991, which included liberalization, privatization, and globalization, were primarily triggered by:',
 '[{"text":"A severe balance of payments crisis","isCorrect":true},{"text":"A stock market boom","isCorrect":false},{"text":"A sudden increase in exports","isCorrect":false},{"text":"A drop in oil prices","isCorrect":false}]'::jsonb,
 null, 'The 1991 reforms were primarily triggered by a severe balance of payments crisis, which forced India to seek external assistance and restructure its economic policy.'),
('Class 12 Commerce', 'Economics', 'Government Budget, Balance of Payments and Indian Economic Development', 'hard', 'mcq',
 'Government expenditure that creates assets or reduces liabilities, such as spending on infrastructure, is classified as:',
 '[{"text":"Revenue expenditure","isCorrect":false},{"text":"Capital expenditure","isCorrect":true},{"text":"Transfer payment only","isCorrect":false},{"text":"Subsidy only","isCorrect":false}]'::jsonb,
 null, 'Capital expenditure either creates assets (like infrastructure) or reduces the government''s liabilities.'),

-- English: Advanced Grammar (Modals, Voice) and Note-Making
('Class 12 Commerce', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'easy', 'mcq',
 'Choose the correct modal expressing advice: "You ___ save some money for emergencies."',
 '[{"text":"should","isCorrect":true},{"text":"can","isCorrect":false},{"text":"may","isCorrect":false},{"text":"will","isCorrect":false}]'::jsonb,
 null, '''Should'' is used to give advice, correctly fitting this recommendation.'),
('Class 12 Commerce', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'medium', 'mcq',
 'Change into passive voice: "The company launched a new product last month."',
 '[{"text":"A new product was launched by the company last month.","isCorrect":true},{"text":"A new product is launched by the company last month.","isCorrect":false},{"text":"A new product has launched by the company last month.","isCorrect":false},{"text":"A new product launches by the company last month.","isCorrect":false}]'::jsonb,
 null, 'The past tense active ''launched'' becomes ''was launched'' in the passive voice.'),
('Class 12 Commerce', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'hard', 'mcq',
 'In note-making, the standard abbreviation for "approximately" is:',
 '[{"text":"approx.","isCorrect":true},{"text":"apprx","isCorrect":false},{"text":"aprx.","isCorrect":false},{"text":"app.","isCorrect":false}]'::jsonb,
 null, '"approx." is the standard, widely used abbreviation for "approximately" in note-making.'),
('Class 12 Commerce', 'English', 'Advanced Grammar (Modals, Voice) and Note-Making', 'hard', 'mcq',
 'Choose the correct modal expressing a polite request in formal writing: "___ you kindly forward the attached report?"',
 '[{"text":"Would","isCorrect":true},{"text":"Must","isCorrect":false},{"text":"Shall","isCorrect":false},{"text":"Can''t","isCorrect":false}]'::jsonb,
 null, '''Would'' is used to make a polite, formal request.'),

-- English: Reading Comprehension and Vocabulary
('Class 12 Commerce', 'English', 'Reading Comprehension and Vocabulary', 'easy', 'mcq',
 'Read: "The company''s quarterly earnings exceeded analyst expectations, sending its stock price higher." What happened to the stock price?',
 '[{"text":"It fell","isCorrect":false},{"text":"It rose","isCorrect":true},{"text":"It stayed the same","isCorrect":false},{"text":"It was suspended","isCorrect":false}]'::jsonb,
 null, 'The passage says the earnings exceeded expectations, sending the stock price higher.'),
('Class 12 Commerce', 'English', 'Reading Comprehension and Vocabulary', 'medium', 'mcq',
 'A word that means the same as ''viable'' is:',
 '[{"text":"Impossible","isCorrect":false},{"text":"Feasible","isCorrect":true},{"text":"Risky","isCorrect":false},{"text":"Outdated","isCorrect":false}]'::jsonb,
 null, '''Viable'' means capable of working successfully, matching ''feasible''.'),
('Class 12 Commerce', 'English', 'Reading Comprehension and Vocabulary', 'hard', 'mcq',
 'A word that means the opposite of ''austerity'' (in an economic context) is:',
 '[{"text":"Frugality","isCorrect":false},{"text":"Extravagance","isCorrect":true},{"text":"Thriftiness","isCorrect":false},{"text":"Restraint","isCorrect":false}]'::jsonb,
 null, '''Austerity'' refers to strict economizing; its opposite is ''extravagance'', meaning excessive spending.'),
('Class 12 Commerce', 'English', 'Reading Comprehension and Vocabulary', 'hard', 'mcq',
 'Read: "The merger negotiations stalled over valuation disagreements, leaving both parties at an impasse." What does ''impasse'' suggest about the negotiations?',
 '[{"text":"They were progressing smoothly","isCorrect":false},{"text":"They had reached a deadlock","isCorrect":true},{"text":"They were concluded successfully","isCorrect":false},{"text":"They had not yet started","isCorrect":false}]'::jsonb,
 null, '''Impasse'' means a situation with no way forward, indicating the negotiations had reached a deadlock.');
