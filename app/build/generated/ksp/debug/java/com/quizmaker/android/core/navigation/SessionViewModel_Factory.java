package com.quizmaker.android.core.navigation;

import com.quizmaker.android.repository.AuthRepository;
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
public final class SessionViewModel_Factory implements Factory<SessionViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public SessionViewModel_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public SessionViewModel get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static SessionViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider) {
    return new SessionViewModel_Factory(authRepositoryProvider);
  }

  public static SessionViewModel newInstance(AuthRepository authRepository) {
    return new SessionViewModel(authRepository);
  }
}
