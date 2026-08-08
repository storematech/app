package com.quizmaker.android.ui.questionbank;

import com.quizmaker.android.repository.AuthRepository;
import com.quizmaker.android.repository.QuestionRepository;
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
public final class QuestionBankViewModel_Factory implements Factory<QuestionBankViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<QuestionRepository> questionRepositoryProvider;

  public QuestionBankViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<QuestionRepository> questionRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.questionRepositoryProvider = questionRepositoryProvider;
  }

  @Override
  public QuestionBankViewModel get() {
    return newInstance(authRepositoryProvider.get(), questionRepositoryProvider.get());
  }

  public static QuestionBankViewModel_Factory create(
      Provider<AuthRepository> authRepositoryProvider,
      Provider<QuestionRepository> questionRepositoryProvider) {
    return new QuestionBankViewModel_Factory(authRepositoryProvider, questionRepositoryProvider);
  }

  public static QuestionBankViewModel newInstance(AuthRepository authRepository,
      QuestionRepository questionRepository) {
    return new QuestionBankViewModel(authRepository, questionRepository);
  }
}
