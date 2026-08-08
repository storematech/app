package com.quizmaker.android.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AiQuizRepository_Factory implements Factory<AiQuizRepository> {
  private final Provider<SupabaseClient> supabaseProvider;

  private final Provider<QuestionRepository> questionRepositoryProvider;

  public AiQuizRepository_Factory(Provider<SupabaseClient> supabaseProvider,
      Provider<QuestionRepository> questionRepositoryProvider) {
    this.supabaseProvider = supabaseProvider;
    this.questionRepositoryProvider = questionRepositoryProvider;
  }

  @Override
  public AiQuizRepository get() {
    return newInstance(supabaseProvider.get(), questionRepositoryProvider.get());
  }

  public static AiQuizRepository_Factory create(Provider<SupabaseClient> supabaseProvider,
      Provider<QuestionRepository> questionRepositoryProvider) {
    return new AiQuizRepository_Factory(supabaseProvider, questionRepositoryProvider);
  }

  public static AiQuizRepository newInstance(SupabaseClient supabase,
      QuestionRepository questionRepository) {
    return new AiQuizRepository(supabase, questionRepository);
  }
}
