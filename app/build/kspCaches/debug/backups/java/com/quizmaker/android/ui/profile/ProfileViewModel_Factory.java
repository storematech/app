package com.quizmaker.android.ui.profile;

import com.quizmaker.android.repository.AuthRepository;
import com.quizmaker.android.repository.ProfileRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  public ProfileViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(authRepositoryProvider.get(), profileRepositoryProvider.get());
  }

  public static ProfileViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new ProfileViewModel_Factory(authRepositoryProvider, profileRepositoryProvider);
  }

  public static ProfileViewModel newInstance(AuthRepository authRepository,
      ProfileRepository profileRepository) {
    return new ProfileViewModel(authRepository, profileRepository);
  }
}
