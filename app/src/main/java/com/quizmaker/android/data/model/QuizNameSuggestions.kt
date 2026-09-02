package com.quizmaker.android.data.model

/** One canned quiz title+description pair — tap-to-fill suggestions shown on Create Quiz's
 *  Details step so a new/exploring user can start a quiz without having to think of a title
 *  themselves. See QUIZ_NAME_SUGGESTIONS. */
data class QuizNameSuggestion(val title: String, val description: String)

/** 50 ready-to-use quiz title+description pairs spanning common school subjects and levels, shown
 *  5 random at a time (see CreateQuizViewModel.reshuffleQuizNameSuggestions) as small tap-to-fill
 *  chips on the Details step — purely a starting point, not tied to any generated content. */
val QUIZ_NAME_SUGGESTIONS = listOf(
    QuizNameSuggestion("Environmental Science Basics", "Test students' understanding of ecosystems, pollution, and conservation."),
    QuizNameSuggestion("World War II Timeline", "Check recall of major events, dates, and figures from World War II."),
    QuizNameSuggestion("Algebra Fundamentals", "Assess skills with linear equations, expressions, and basic algebraic operations."),
    QuizNameSuggestion("Human Digestive System", "Quiz on organs, functions, and processes involved in human digestion."),
    QuizNameSuggestion("Grammar & Punctuation", "Test knowledge of sentence structure, punctuation rules, and common grammar mistakes."),
    QuizNameSuggestion("Indian Geography Quiz", "Check knowledge of states, rivers, mountains, and capitals of India."),
    QuizNameSuggestion("Photosynthesis Basics", "Test understanding of how plants convert light into energy."),
    QuizNameSuggestion("Basic Computer Concepts", "Assess familiarity with hardware, software, and common computer terms."),
    QuizNameSuggestion("Periodic Table Quiz", "Check knowledge of elements, groups, periods, and atomic properties."),
    QuizNameSuggestion("Fractions & Decimals", "Test skills converting between fractions, decimals, and percentages."),
    QuizNameSuggestion("Ancient Civilizations", "Quiz on Egypt, Greece, Rome, and other early world civilizations."),
    QuizNameSuggestion("Solar System Quiz", "Test knowledge of planets, moons, and other objects in our solar system."),
    QuizNameSuggestion("Parts of Speech", "Assess understanding of nouns, verbs, adjectives, and other word types."),
    QuizNameSuggestion("Cell Biology Basics", "Check understanding of cell structure, organelles, and basic cell functions."),
    QuizNameSuggestion("Indian Freedom Struggle", "Test knowledge of key events and leaders of India's independence movement."),
    QuizNameSuggestion("Geometry Shapes & Angles", "Assess recognition of shapes, angle types, and basic geometric properties."),
    QuizNameSuggestion("English Vocabulary Builder", "Check word meanings, synonyms, antonyms, and usage in context."),
    QuizNameSuggestion("Basic Chemistry Reactions", "Test understanding of chemical reactions, compounds, and basic formulas."),
    QuizNameSuggestion("Water Cycle Quiz", "Assess understanding of evaporation, condensation, and precipitation."),
    QuizNameSuggestion("Famous Scientists & Discoveries", "Test knowledge of key scientists and their major contributions."),
    QuizNameSuggestion("Multiplication Tables", "Quick recall check for multiplication facts up to 12."),
    QuizNameSuggestion("Reading Comprehension Basics", "Assess ability to understand and interpret short passages."),
    QuizNameSuggestion("Human Skeletal System", "Test knowledge of bones, joints, and the structure of the human skeleton."),
    QuizNameSuggestion("World Capitals Quiz", "Check knowledge of capital cities from around the world."),
    QuizNameSuggestion("Basic Programming Concepts", "Assess familiarity with variables, loops, and conditionals."),
    QuizNameSuggestion("Weather & Climate Basics", "Test understanding of weather patterns and climate concepts."),
    QuizNameSuggestion("Roman Numerals Quiz", "Check ability to read and convert Roman numerals."),
    QuizNameSuggestion("Food & Nutrition Basics", "Assess knowledge of food groups, nutrients, and healthy eating habits."),
    QuizNameSuggestion("Basic Trigonometry", "Test understanding of sine, cosine, tangent, and right triangles."),
    QuizNameSuggestion("Indian National Symbols", "Check knowledge of the national flag, anthem, animal, and other symbols."),
    QuizNameSuggestion("States of Matter", "Test understanding of solids, liquids, gases, and phase changes."),
    QuizNameSuggestion("Common Idioms & Phrases", "Assess understanding of everyday English idioms and their meanings."),
    QuizNameSuggestion("Basic Economics Concepts", "Check understanding of supply, demand, and other core economic ideas."),
    QuizNameSuggestion("Human Respiratory System", "Test knowledge of lungs, breathing, and gas exchange in the body."),
    QuizNameSuggestion("Continents & Oceans", "Assess knowledge of the world's continents and oceans."),
    QuizNameSuggestion("Basic Probability & Statistics", "Test understanding of averages, probability, and simple data analysis."),
    QuizNameSuggestion("Renewable Energy Sources", "Check knowledge of solar, wind, and other renewable energy types."),
    QuizNameSuggestion("Shakespeare & Classic Literature", "Test familiarity with famous plays, authors, and literary works."),
    QuizNameSuggestion("Basic First Aid Knowledge", "Assess understanding of common first-aid steps and safety practices."),
    QuizNameSuggestion("Indian Constitution Basics", "Check knowledge of fundamental rights, duties, and the structure of government."),
    QuizNameSuggestion("Time & Measurement", "Test skills reading clocks, calendars, and basic units of measurement."),
    QuizNameSuggestion("Animal Kingdom Classification", "Assess understanding of how animals are grouped and classified."),
    QuizNameSuggestion("Basic Physics Concepts", "Test understanding of force, motion, energy, and simple machines."),
    QuizNameSuggestion("Common Programming Languages", "Check familiarity with popular languages and their common uses."),
    QuizNameSuggestion("Mythology & Folklore", "Test knowledge of myths, legends, and folk tales from around the world."),
    QuizNameSuggestion("Basic Map Reading Skills", "Assess ability to read maps, scales, and directions."),
    QuizNameSuggestion("Healthy Habits Quiz", "Check understanding of exercise, sleep, hygiene, and healthy routines."),
    QuizNameSuggestion("Basic Business Terms", "Test familiarity with common business and workplace vocabulary."),
    QuizNameSuggestion("Space Exploration History", "Assess knowledge of major milestones in space exploration."),
    QuizNameSuggestion("General Knowledge Mix", "A broad mix of general knowledge questions across multiple topics.")
)
