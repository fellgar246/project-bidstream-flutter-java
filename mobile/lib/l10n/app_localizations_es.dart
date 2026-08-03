// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Spanish Castilian (`es`).
class AppLocalizationsEs extends AppLocalizations {
  AppLocalizationsEs([String locale = 'es']) : super(locale);

  @override
  String get appTitle => 'BidStream';

  @override
  String get homeTitle => 'BidStream';

  @override
  String get homeSubtitle => 'Plataforma de subastas en vivo';

  @override
  String get viewCategories => 'Ver categorías';

  @override
  String get viewProfile => 'Perfil';

  @override
  String get categoriesTitle => 'Categorías';

  @override
  String get categoriesLoading => 'Cargando categorías...';

  @override
  String get categoriesError => 'No se pudieron cargar las categorías';

  @override
  String get categoriesEmpty => 'No hay categorías disponibles';

  @override
  String get retry => 'Reintentar';

  @override
  String get splashLoading => 'Restaurando sesión...';

  @override
  String get loginTitle => 'Iniciar sesión';

  @override
  String get registerTitle => 'Crear cuenta';

  @override
  String get profileTitle => 'Perfil';

  @override
  String get emailLabel => 'Correo electrónico';

  @override
  String get passwordLabel => 'Contraseña';

  @override
  String get displayNameLabel => 'Nombre para mostrar';

  @override
  String get emailRequired => 'El correo es obligatorio';

  @override
  String get passwordRequired => 'La contraseña es obligatoria';

  @override
  String get displayNameRequired => 'El nombre es obligatorio';

  @override
  String get passwordMinLength =>
      'La contraseña debe tener al menos 10 caracteres';

  @override
  String get loginAction => 'Iniciar sesión';

  @override
  String get registerAction => 'Crear cuenta';

  @override
  String get logoutAction => 'Cerrar sesión';

  @override
  String get goToRegister => '¿No tienes cuenta? Regístrate';

  @override
  String get goToLogin => '¿Ya tienes cuenta? Inicia sesión';

  @override
  String get loginError => 'No se pudo iniciar sesión';

  @override
  String get registerError => 'No se pudo crear la cuenta';

  @override
  String get becomeSeller => 'Convertirme en vendedor';

  @override
  String get sellerApplicationError => 'No se pudo solicitar ser vendedor';

  @override
  String get rolesLabel => 'Roles';

  @override
  String get lotsTitle => 'Lotes';

  @override
  String get lotsError => 'No se pudieron cargar los lotes';

  @override
  String get lotsEmpty => 'No hay lotes disponibles';

  @override
  String get lotsFiltersTitle => 'Filtros';

  @override
  String get lotsFilterStatus => 'Estado';

  @override
  String get lotsFilterSort => 'Orden';

  @override
  String get lotsApplyFilters => 'Aplicar filtros';

  @override
  String get lotsSearchHint => 'Buscar lotes';

  @override
  String get lotsClearFilters => 'Limpiar filtros';

  @override
  String lotsSearchEmpty(String query) {
    return 'Sin resultados para \"$query\"';
  }

  @override
  String get lotDetailTitle => 'Detalle del lote';

  @override
  String get lotCurrentPrice => 'Precio actual';

  @override
  String get lotStatus => 'Estado';

  @override
  String get lotSeller => 'Vendedor';

  @override
  String get lotEdit => 'Editar lote';

  @override
  String get lotEditTitle => 'Administrar imágenes';

  @override
  String get lotImagesTitle => 'Imágenes';

  @override
  String get lotAddImage => 'Agregar imagen';

  @override
  String get lotUploading => 'Subiendo';

  @override
  String get lotConfirming => 'Confirmando';

  @override
  String get lotPickGallery => 'Galería';

  @override
  String get lotPickCamera => 'Cámara';

  @override
  String get lotCameraPermissionDenied =>
      'Se necesita permiso de cámara para tomar fotos.';

  @override
  String get lotOpenSettings => 'Abrir ajustes';

  @override
  String get lotReorderHint => 'Arrastra para reordenar las portadas';

  @override
  String get lotReorderFailed => 'No se pudo guardar el orden';

  @override
  String get lotImagePosition => 'Imagen';

  @override
  String get lotFormTitle => 'Nuevo lote';

  @override
  String get lotTitleLabel => 'Título';

  @override
  String get lotDescriptionLabel => 'Descripción';

  @override
  String get lotStartingPriceLabel => 'Precio inicial';

  @override
  String get lotMinIncrementLabel => 'Incremento mínimo';

  @override
  String get lotReservePriceLabel => 'Precio de reserva (opcional)';

  @override
  String get lotCreateAction => 'Crear lote';

  @override
  String get sellerLotsTitle => 'Mis lotes';

  @override
  String get viewLots => 'Explorar lotes';

  @override
  String get viewSellerLots => 'Mis lotes';

  @override
  String get bidTitle => 'Hacer una puja';

  @override
  String get bidAmountLabel => 'Monto de tu puja';

  @override
  String get bidPlaceAction => 'Pujar';

  @override
  String get bidMinIncrement => 'Incremento mínimo';

  @override
  String get bidInvalidAmount => 'Ingresa un monto válido';

  @override
  String bidCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count pujas',
      one: '1 puja',
      zero: 'Sin pujas',
    );
    return '$_temp0';
  }

  @override
  String get liveAuctionTitle => 'Subasta en vivo';

  @override
  String get liveCountdown => 'Tiempo restante';

  @override
  String get liveExtendedBanner => 'Tiempo extendido';

  @override
  String get liveReconnecting => 'Reconectando…';

  @override
  String get liveDisconnected => 'Conexión perdida';

  @override
  String get liveWatchAction => 'Ver en vivo';

  @override
  String get liveYouWon => '🎉 ¡Ganaste!';

  @override
  String get liveClosedNoSale => 'Subasta cerrada sin venta';

  @override
  String closesIn(int hours, int minutes) {
    return 'Cierra en $hours h $minutes min';
  }

  @override
  String closesInMinutes(int minutes, int seconds) {
    return 'Cierra en $minutes min $seconds s';
  }

  @override
  String get notificationsTitle => 'Notificaciones';

  @override
  String get markAllRead => 'Marcar todo leído';

  @override
  String get noNotifications => 'Aún no hay notificaciones';

  @override
  String get notificationYouWon => 'Ganaste la subasta';

  @override
  String get notificationOutbid => 'Te superaron en una puja';

  @override
  String get notificationLotSold => 'Tu lote se vendió';

  @override
  String get notificationLotNoSale => 'Lote cerrado sin venta';

  @override
  String get notificationLotStarted => 'Tu lote está en vivo';

  @override
  String get settingsTitle => 'Ajustes';

  @override
  String get languageLabel => 'Idioma';

  @override
  String get languageSystem => 'Según el sistema';

  @override
  String get languageSpanish => 'Español';

  @override
  String get languageEnglish => 'Inglés';

  @override
  String offlineBanner(String age) {
    return 'Sin conexión — datos de hace $age';
  }

  @override
  String get stalePriceWarning => 'Precio potencialmente desactualizado';

  @override
  String get clearCache => 'Limpiar caché';

  @override
  String get cacheCleared => 'Caché eliminada';
}
