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
public final class QuizDetailViewRepository_Factory implements Factory<QuizDetailViewRepository> {
  private final Provider<SupabaseClient> supabaseProvider;

  public QuizDetailViewRepository_Factory(Provider<SupabaseClient> supabaseProvider) {
    this.supabaseProvider = supabaseProvider;
  }

  @Override
  public QuizDetailViewRepository get() {
    return newInstance(supabaseProvider.get());
  }

  public static QuizDetailViewRepository_Factory create(Provider<SupabaseClient> supabaseProvider) {
    return new QuizDetailViewRepository_Factory(supabaseProvider);
  }

  public static QuizDetailViewRepository newInstance(SupabaseClient supabase) {
    return new QuizDetailViewRepository(supabase);
  }
}
