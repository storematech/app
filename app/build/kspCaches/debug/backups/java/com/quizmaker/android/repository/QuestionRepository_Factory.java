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
public final class QuestionRepository_Factory implements Factory<QuestionRepository> {
  private final Provider<SupabaseClient> supabaseProvider;

  public QuestionRepository_Factory(Provider<SupabaseClient> supabaseProvider) {
    this.supabaseProvider = supabaseProvider;
  }

  @Override
  public QuestionRepository get() {
    return newInstance(supabaseProvider.get());
  }

  public static QuestionRepository_Factory create(Provider<SupabaseClient> supabaseProvider) {
    return new QuestionRepository_Factory(supabaseProvider);
  }

  public static QuestionRepository newInstance(SupabaseClient supabase) {
    return new QuestionRepository(supabase);
  }
}
