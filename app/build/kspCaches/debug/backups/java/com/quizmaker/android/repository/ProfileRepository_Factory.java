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
public final class ProfileRepository_Factory implements Factory<ProfileRepository> {
  private final Provider<SupabaseClient> supabaseProvider;

  public ProfileRepository_Factory(Provider<SupabaseClient> supabaseProvider) {
    this.supabaseProvider = supabaseProvider;
  }

  @Override
  public ProfileRepository get() {
    return newInstance(supabaseProvider.get());
  }

  public static ProfileRepository_Factory create(Provider<SupabaseClient> supabaseProvider) {
    return new ProfileRepository_Factory(supabaseProvider);
  }

  public static ProfileRepository newInstance(SupabaseClient supabase) {
    return new ProfileRepository(supabase);
  }
}
