import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_es.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('es'),
  ];

  /// No description provided for @appTitle.
  ///
  /// In es, this message translates to:
  /// **'BidStream'**
  String get appTitle;

  /// No description provided for @homeTitle.
  ///
  /// In es, this message translates to:
  /// **'BidStream'**
  String get homeTitle;

  /// No description provided for @homeSubtitle.
  ///
  /// In es, this message translates to:
  /// **'Plataforma de subastas en vivo'**
  String get homeSubtitle;

  /// No description provided for @viewCategories.
  ///
  /// In es, this message translates to:
  /// **'Ver categorías'**
  String get viewCategories;

  /// No description provided for @viewProfile.
  ///
  /// In es, this message translates to:
  /// **'Perfil'**
  String get viewProfile;

  /// No description provided for @categoriesTitle.
  ///
  /// In es, this message translates to:
  /// **'Categorías'**
  String get categoriesTitle;

  /// No description provided for @categoriesLoading.
  ///
  /// In es, this message translates to:
  /// **'Cargando categorías...'**
  String get categoriesLoading;

  /// No description provided for @categoriesError.
  ///
  /// In es, this message translates to:
  /// **'No se pudieron cargar las categorías'**
  String get categoriesError;

  /// No description provided for @categoriesEmpty.
  ///
  /// In es, this message translates to:
  /// **'No hay categorías disponibles'**
  String get categoriesEmpty;

  /// No description provided for @retry.
  ///
  /// In es, this message translates to:
  /// **'Reintentar'**
  String get retry;

  /// No description provided for @splashLoading.
  ///
  /// In es, this message translates to:
  /// **'Restaurando sesión...'**
  String get splashLoading;

  /// No description provided for @loginTitle.
  ///
  /// In es, this message translates to:
  /// **'Iniciar sesión'**
  String get loginTitle;

  /// No description provided for @registerTitle.
  ///
  /// In es, this message translates to:
  /// **'Crear cuenta'**
  String get registerTitle;

  /// No description provided for @profileTitle.
  ///
  /// In es, this message translates to:
  /// **'Perfil'**
  String get profileTitle;

  /// No description provided for @emailLabel.
  ///
  /// In es, this message translates to:
  /// **'Correo electrónico'**
  String get emailLabel;

  /// No description provided for @passwordLabel.
  ///
  /// In es, this message translates to:
  /// **'Contraseña'**
  String get passwordLabel;

  /// No description provided for @displayNameLabel.
  ///
  /// In es, this message translates to:
  /// **'Nombre para mostrar'**
  String get displayNameLabel;

  /// No description provided for @emailRequired.
  ///
  /// In es, this message translates to:
  /// **'El correo es obligatorio'**
  String get emailRequired;

  /// No description provided for @passwordRequired.
  ///
  /// In es, this message translates to:
  /// **'La contraseña es obligatoria'**
  String get passwordRequired;

  /// No description provided for @displayNameRequired.
  ///
  /// In es, this message translates to:
  /// **'El nombre es obligatorio'**
  String get displayNameRequired;

  /// No description provided for @passwordMinLength.
  ///
  /// In es, this message translates to:
  /// **'La contraseña debe tener al menos 10 caracteres'**
  String get passwordMinLength;

  /// No description provided for @loginAction.
  ///
  /// In es, this message translates to:
  /// **'Iniciar sesión'**
  String get loginAction;

  /// No description provided for @registerAction.
  ///
  /// In es, this message translates to:
  /// **'Crear cuenta'**
  String get registerAction;

  /// No description provided for @logoutAction.
  ///
  /// In es, this message translates to:
  /// **'Cerrar sesión'**
  String get logoutAction;

  /// No description provided for @goToRegister.
  ///
  /// In es, this message translates to:
  /// **'¿No tienes cuenta? Regístrate'**
  String get goToRegister;

  /// No description provided for @goToLogin.
  ///
  /// In es, this message translates to:
  /// **'¿Ya tienes cuenta? Inicia sesión'**
  String get goToLogin;

  /// No description provided for @loginError.
  ///
  /// In es, this message translates to:
  /// **'No se pudo iniciar sesión'**
  String get loginError;

  /// No description provided for @registerError.
  ///
  /// In es, this message translates to:
  /// **'No se pudo crear la cuenta'**
  String get registerError;

  /// No description provided for @becomeSeller.
  ///
  /// In es, this message translates to:
  /// **'Convertirme en vendedor'**
  String get becomeSeller;

  /// No description provided for @sellerApplicationError.
  ///
  /// In es, this message translates to:
  /// **'No se pudo solicitar ser vendedor'**
  String get sellerApplicationError;

  /// No description provided for @rolesLabel.
  ///
  /// In es, this message translates to:
  /// **'Roles'**
  String get rolesLabel;

  /// No description provided for @lotsTitle.
  ///
  /// In es, this message translates to:
  /// **'Lotes'**
  String get lotsTitle;

  /// No description provided for @lotsError.
  ///
  /// In es, this message translates to:
  /// **'No se pudieron cargar los lotes'**
  String get lotsError;

  /// No description provided for @lotsEmpty.
  ///
  /// In es, this message translates to:
  /// **'No hay lotes disponibles'**
  String get lotsEmpty;

  /// No description provided for @lotsFiltersTitle.
  ///
  /// In es, this message translates to:
  /// **'Filtros'**
  String get lotsFiltersTitle;

  /// No description provided for @lotsFilterStatus.
  ///
  /// In es, this message translates to:
  /// **'Estado'**
  String get lotsFilterStatus;

  /// No description provided for @lotsFilterSort.
  ///
  /// In es, this message translates to:
  /// **'Orden'**
  String get lotsFilterSort;

  /// No description provided for @lotsApplyFilters.
  ///
  /// In es, this message translates to:
  /// **'Aplicar filtros'**
  String get lotsApplyFilters;

  /// No description provided for @lotsSearchHint.
  ///
  /// In es, this message translates to:
  /// **'Buscar lotes'**
  String get lotsSearchHint;

  /// No description provided for @lotsClearFilters.
  ///
  /// In es, this message translates to:
  /// **'Limpiar filtros'**
  String get lotsClearFilters;

  /// No description provided for @lotsSearchEmpty.
  ///
  /// In es, this message translates to:
  /// **'Sin resultados para \"{query}\"'**
  String lotsSearchEmpty(String query);

  /// No description provided for @lotDetailTitle.
  ///
  /// In es, this message translates to:
  /// **'Detalle del lote'**
  String get lotDetailTitle;

  /// No description provided for @lotCurrentPrice.
  ///
  /// In es, this message translates to:
  /// **'Precio actual'**
  String get lotCurrentPrice;

  /// No description provided for @lotStatus.
  ///
  /// In es, this message translates to:
  /// **'Estado'**
  String get lotStatus;

  /// No description provided for @lotSeller.
  ///
  /// In es, this message translates to:
  /// **'Vendedor'**
  String get lotSeller;

  /// No description provided for @lotEdit.
  ///
  /// In es, this message translates to:
  /// **'Editar lote'**
  String get lotEdit;

  /// No description provided for @lotEditTitle.
  ///
  /// In es, this message translates to:
  /// **'Administrar imágenes'**
  String get lotEditTitle;

  /// No description provided for @lotImagesTitle.
  ///
  /// In es, this message translates to:
  /// **'Imágenes'**
  String get lotImagesTitle;

  /// No description provided for @lotAddImage.
  ///
  /// In es, this message translates to:
  /// **'Agregar imagen'**
  String get lotAddImage;

  /// No description provided for @lotUploading.
  ///
  /// In es, this message translates to:
  /// **'Subiendo'**
  String get lotUploading;

  /// No description provided for @lotConfirming.
  ///
  /// In es, this message translates to:
  /// **'Confirmando'**
  String get lotConfirming;

  /// No description provided for @lotPickGallery.
  ///
  /// In es, this message translates to:
  /// **'Galería'**
  String get lotPickGallery;

  /// No description provided for @lotPickCamera.
  ///
  /// In es, this message translates to:
  /// **'Cámara'**
  String get lotPickCamera;

  /// No description provided for @lotCameraPermissionDenied.
  ///
  /// In es, this message translates to:
  /// **'Se necesita permiso de cámara para tomar fotos.'**
  String get lotCameraPermissionDenied;

  /// No description provided for @lotOpenSettings.
  ///
  /// In es, this message translates to:
  /// **'Abrir ajustes'**
  String get lotOpenSettings;

  /// No description provided for @lotReorderHint.
  ///
  /// In es, this message translates to:
  /// **'Arrastra para reordenar las portadas'**
  String get lotReorderHint;

  /// No description provided for @lotReorderFailed.
  ///
  /// In es, this message translates to:
  /// **'No se pudo guardar el orden'**
  String get lotReorderFailed;

  /// No description provided for @lotImagePosition.
  ///
  /// In es, this message translates to:
  /// **'Imagen'**
  String get lotImagePosition;

  /// No description provided for @lotFormTitle.
  ///
  /// In es, this message translates to:
  /// **'Nuevo lote'**
  String get lotFormTitle;

  /// No description provided for @lotTitleLabel.
  ///
  /// In es, this message translates to:
  /// **'Título'**
  String get lotTitleLabel;

  /// No description provided for @lotDescriptionLabel.
  ///
  /// In es, this message translates to:
  /// **'Descripción'**
  String get lotDescriptionLabel;

  /// No description provided for @lotStartingPriceLabel.
  ///
  /// In es, this message translates to:
  /// **'Precio inicial'**
  String get lotStartingPriceLabel;

  /// No description provided for @lotMinIncrementLabel.
  ///
  /// In es, this message translates to:
  /// **'Incremento mínimo'**
  String get lotMinIncrementLabel;

  /// No description provided for @lotReservePriceLabel.
  ///
  /// In es, this message translates to:
  /// **'Precio de reserva (opcional)'**
  String get lotReservePriceLabel;

  /// No description provided for @lotCreateAction.
  ///
  /// In es, this message translates to:
  /// **'Crear lote'**
  String get lotCreateAction;

  /// No description provided for @sellerLotsTitle.
  ///
  /// In es, this message translates to:
  /// **'Mis lotes'**
  String get sellerLotsTitle;

  /// No description provided for @viewLots.
  ///
  /// In es, this message translates to:
  /// **'Explorar lotes'**
  String get viewLots;

  /// No description provided for @viewSellerLots.
  ///
  /// In es, this message translates to:
  /// **'Mis lotes'**
  String get viewSellerLots;

  /// No description provided for @bidTitle.
  ///
  /// In es, this message translates to:
  /// **'Hacer una puja'**
  String get bidTitle;

  /// No description provided for @bidAmountLabel.
  ///
  /// In es, this message translates to:
  /// **'Monto de tu puja'**
  String get bidAmountLabel;

  /// No description provided for @bidPlaceAction.
  ///
  /// In es, this message translates to:
  /// **'Pujar'**
  String get bidPlaceAction;

  /// No description provided for @bidMinIncrement.
  ///
  /// In es, this message translates to:
  /// **'Incremento mínimo'**
  String get bidMinIncrement;

  /// No description provided for @bidInvalidAmount.
  ///
  /// In es, this message translates to:
  /// **'Ingresa un monto válido'**
  String get bidInvalidAmount;

  /// No description provided for @bidCount.
  ///
  /// In es, this message translates to:
  /// **'{count, plural, =0{Sin pujas} =1{1 puja} other{{count} pujas}}'**
  String bidCount(int count);

  /// No description provided for @liveAuctionTitle.
  ///
  /// In es, this message translates to:
  /// **'Subasta en vivo'**
  String get liveAuctionTitle;

  /// No description provided for @liveCountdown.
  ///
  /// In es, this message translates to:
  /// **'Tiempo restante'**
  String get liveCountdown;

  /// No description provided for @liveExtendedBanner.
  ///
  /// In es, this message translates to:
  /// **'Tiempo extendido'**
  String get liveExtendedBanner;

  /// No description provided for @liveReconnecting.
  ///
  /// In es, this message translates to:
  /// **'Reconectando…'**
  String get liveReconnecting;

  /// No description provided for @liveDisconnected.
  ///
  /// In es, this message translates to:
  /// **'Conexión perdida'**
  String get liveDisconnected;

  /// No description provided for @liveWatchAction.
  ///
  /// In es, this message translates to:
  /// **'Ver en vivo'**
  String get liveWatchAction;

  /// No description provided for @liveYouWon.
  ///
  /// In es, this message translates to:
  /// **'🎉 ¡Ganaste!'**
  String get liveYouWon;

  /// No description provided for @liveClosedNoSale.
  ///
  /// In es, this message translates to:
  /// **'Subasta cerrada sin venta'**
  String get liveClosedNoSale;

  /// No description provided for @closesIn.
  ///
  /// In es, this message translates to:
  /// **'Cierra en {hours} h {minutes} min'**
  String closesIn(int hours, int minutes);

  /// No description provided for @closesInMinutes.
  ///
  /// In es, this message translates to:
  /// **'Cierra en {minutes} min {seconds} s'**
  String closesInMinutes(int minutes, int seconds);

  /// No description provided for @notificationsTitle.
  ///
  /// In es, this message translates to:
  /// **'Notificaciones'**
  String get notificationsTitle;

  /// No description provided for @markAllRead.
  ///
  /// In es, this message translates to:
  /// **'Marcar todo leído'**
  String get markAllRead;

  /// No description provided for @noNotifications.
  ///
  /// In es, this message translates to:
  /// **'Aún no hay notificaciones'**
  String get noNotifications;

  /// No description provided for @notificationYouWon.
  ///
  /// In es, this message translates to:
  /// **'Ganaste la subasta'**
  String get notificationYouWon;

  /// No description provided for @notificationOutbid.
  ///
  /// In es, this message translates to:
  /// **'Te superaron en una puja'**
  String get notificationOutbid;

  /// No description provided for @notificationLotSold.
  ///
  /// In es, this message translates to:
  /// **'Tu lote se vendió'**
  String get notificationLotSold;

  /// No description provided for @notificationLotNoSale.
  ///
  /// In es, this message translates to:
  /// **'Lote cerrado sin venta'**
  String get notificationLotNoSale;

  /// No description provided for @notificationLotStarted.
  ///
  /// In es, this message translates to:
  /// **'Tu lote está en vivo'**
  String get notificationLotStarted;

  /// No description provided for @settingsTitle.
  ///
  /// In es, this message translates to:
  /// **'Ajustes'**
  String get settingsTitle;

  /// No description provided for @languageLabel.
  ///
  /// In es, this message translates to:
  /// **'Idioma'**
  String get languageLabel;

  /// No description provided for @languageSystem.
  ///
  /// In es, this message translates to:
  /// **'Según el sistema'**
  String get languageSystem;

  /// No description provided for @languageSpanish.
  ///
  /// In es, this message translates to:
  /// **'Español'**
  String get languageSpanish;

  /// No description provided for @languageEnglish.
  ///
  /// In es, this message translates to:
  /// **'Inglés'**
  String get languageEnglish;

  /// No description provided for @offlineBanner.
  ///
  /// In es, this message translates to:
  /// **'Sin conexión — datos de hace {age}'**
  String offlineBanner(String age);

  /// No description provided for @stalePriceWarning.
  ///
  /// In es, this message translates to:
  /// **'Precio potencialmente desactualizado'**
  String get stalePriceWarning;

  /// No description provided for @clearCache.
  ///
  /// In es, this message translates to:
  /// **'Limpiar caché'**
  String get clearCache;

  /// No description provided for @cacheCleared.
  ///
  /// In es, this message translates to:
  /// **'Caché eliminada'**
  String get cacheCleared;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'es'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'es':
      return AppLocalizationsEs();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
