String? _deferredRoute;

void setDeferredRoute(String route) {
  _deferredRoute = route;
}

String? takeDeferredRoute() {
  final route = _deferredRoute;
  _deferredRoute = null;
  return route;
}

String? peekDeferredRoute() => _deferredRoute;
