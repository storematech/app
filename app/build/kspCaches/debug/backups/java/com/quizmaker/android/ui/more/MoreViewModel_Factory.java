package com.quizmaker.android.ui.more;

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
public final class MoreViewModel_Factory implements Factory<MoreViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public MoreViewModel_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public MoreViewModel get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static MoreViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider) {
    return new MoreViewModel_Factory(authRepositoryProvider);
  }

  public static MoreViewModel newInstance(AuthRepository authRepository) {
    return new MoreViewModel(authRepository);
  }
}
