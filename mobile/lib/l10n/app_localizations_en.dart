// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'BidStream';

  @override
  String get homeTitle => 'BidStream';

  @override
  String get homeSubtitle => 'Live auctions platform';

  @override
  String get viewCategories => 'View categories';

  @override
  String get viewProfile => 'Profile';

  @override
  String get categoriesTitle => 'Categories';

  @override
  String get categoriesLoading => 'Loading categories...';

  @override
  String get categoriesError => 'Could not load categories';

  @override
  String get categoriesEmpty => 'No categories available';

  @override
  String get retry => 'Retry';

  @override
  String get splashLoading => 'Restoring session...';

  @override
  String get loginTitle => 'Sign in';

  @override
  String get registerTitle => 'Create account';

  @override
  String get profileTitle => 'Profile';

  @override
  String get emailLabel => 'Email';

  @override
  String get passwordLabel => 'Password';

  @override
  String get displayNameLabel => 'Display name';

  @override
  String get emailRequired => 'Email is required';

  @override
  String get passwordRequired => 'Password is required';

  @override
  String get displayNameRequired => 'Display name is required';

  @override
  String get passwordMinLength => 'Password must be at least 10 characters';

  @override
  String get loginAction => 'Sign in';

  @override
  String get registerAction => 'Create account';

  @override
  String get logoutAction => 'Sign out';

  @override
  String get goToRegister => 'Need an account? Register';

  @override
  String get goToLogin => 'Already have an account? Sign in';

  @override
  String get loginError => 'Could not sign in';

  @override
  String get registerError => 'Could not create account';

  @override
  String get becomeSeller => 'Become a seller';

  @override
  String get sellerApplicationError => 'Could not apply as seller';

  @override
  String get rolesLabel => 'Roles';

  @override
  String get lotsTitle => 'Lots';

  @override
  String get lotsError => 'Could not load lots';

  @override
  String get lotsEmpty => 'No lots available';

  @override
  String get lotsFiltersTitle => 'Filters';

  @override
  String get lotsFilterStatus => 'Status';

  @override
  String get lotsFilterSort => 'Sort';

  @override
  String get lotsApplyFilters => 'Apply filters';

  @override
  String get lotsSearchHint => 'Search lots';

  @override
  String get lotsClearFilters => 'Clear filters';

  @override
  String lotsSearchEmpty(String query) {
    return 'No results for \"$query\"';
  }

  @override
  String get lotDetailTitle => 'Lot detail';

  @override
  String get lotCurrentPrice => 'Current price';

  @override
  String get lotStatus => 'Status';

  @override
  String get lotSeller => 'Seller';

  @override
  String get lotEdit => 'Edit lot';

  @override
  String get lotEditTitle => 'Manage lot images';

  @override
  String get lotImagesTitle => 'Images';

  @override
  String get lotAddImage => 'Add image';

  @override
  String get lotUploading => 'Uploading';

  @override
  String get lotConfirming => 'Confirming';

  @override
  String get lotPickGallery => 'Gallery';

  @override
  String get lotPickCamera => 'Camera';

  @override
  String get lotCameraPermissionDenied =>
      'Camera permission is required to take photos.';

  @override
  String get lotOpenSettings => 'Open settings';

  @override
  String get lotReorderHint => 'Drag to reorder cover images';

  @override
  String get lotReorderFailed => 'Could not save image order';

  @override
  String get lotImagePosition => 'Image';

  @override
  String get lotFormTitle => 'New lot';

  @override
  String get lotTitleLabel => 'Title';

  @override
  String get lotDescriptionLabel => 'Description';

  @override
  String get lotStartingPriceLabel => 'Starting price';

  @override
  String get lotMinIncrementLabel => 'Minimum increment';

  @override
  String get lotReservePriceLabel => 'Reserve price (optional)';

  @override
  String get lotCreateAction => 'Create lot';

  @override
  String get sellerLotsTitle => 'My lots';

  @override
  String get viewLots => 'Browse lots';

  @override
  String get viewSellerLots => 'My lots';

  @override
  String get bidTitle => 'Place a bid';

  @override
  String get bidAmountLabel => 'Your bid amount';

  @override
  String get bidPlaceAction => 'Place bid';

  @override
  String get bidMinIncrement => 'Minimum increment';

  @override
  String get bidInvalidAmount => 'Enter a valid amount';

  @override
  String bidCount(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count bids',
      one: '1 bid',
      zero: 'No bids',
    );
    return '$_temp0';
  }

  @override
  String get liveAuctionTitle => 'Live auction';

  @override
  String get liveCountdown => 'Time left';

  @override
  String get liveExtendedBanner => 'Time extended';

  @override
  String get liveReconnecting => 'Reconnecting…';

  @override
  String get liveDisconnected => 'Connection lost';

  @override
  String get liveWatchAction => 'Watch live';

  @override
  String get liveYouWon => '🎉 You won!';

  @override
  String get liveClosedNoSale => 'Auction closed without sale';

  @override
  String closesIn(int hours, int minutes) {
    return 'Closes in $hours h $minutes min';
  }

  @override
  String closesInMinutes(int minutes, int seconds) {
    return 'Closes in $minutes min $seconds s';
  }

  @override
  String get notificationsTitle => 'Notifications';

  @override
  String get markAllRead => 'Mark all read';

  @override
  String get noNotifications => 'No notifications yet';

  @override
  String get notificationYouWon => 'You won the auction';

  @override
  String get notificationOutbid => 'You were outbid';

  @override
  String get notificationLotSold => 'Your lot sold';

  @override
  String get notificationLotNoSale => 'Lot closed without sale';

  @override
  String get notificationLotStarted => 'Your lot is live';

  @override
  String get settingsTitle => 'Settings';

  @override
  String get languageLabel => 'Language';

  @override
  String get languageSystem => 'System default';

  @override
  String get languageSpanish => 'Spanish';

  @override
  String get languageEnglish => 'English';

  @override
  String offlineBanner(String age) {
    return 'Offline — data from $age ago';
  }

  @override
  String get stalePriceWarning => 'Price may be outdated';

  @override
  String get clearCache => 'Clear cache';

  @override
  String get cacheCleared => 'Cache cleared';
}
