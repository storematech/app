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
public final class QuizTakingRepository_Factory implements Factory<QuizTakingRepository> {
  private final Provider<SupabaseClient> supabaseProvider;

  public QuizTakingRepository_Factory(Provider<SupabaseClient> supabaseProvider) {
    this.supabaseProvider = supabaseProvider;
  }

  @Override
  public QuizTakingRepository get() {
    return newInstance(supabaseProvider.get());
  }

  public static QuizTakingRepository_Factory create(Provider<SupabaseClient> supabaseProvider) {
    return new QuizTakingRepository_Factory(supabaseProvider);
  }

  public static QuizTakingRepository newInstance(SupabaseClient supabase) {
    return new QuizTakingRepository(supabase);
  }
}
