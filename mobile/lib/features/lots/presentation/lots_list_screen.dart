import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/locale_provider.dart';
import '../data/lot_dto.dart';
import '../data/lot_filters.dart';
import '../providers/lots_list_provider.dart';
import '../providers/search_provider.dart';

class LotsListScreen extends ConsumerStatefulWidget {
  const LotsListScreen({super.key, this.initialFilters = const LotFilters()});

  final LotFilters initialFilters;

  @override
  ConsumerState<LotsListScreen> createState() => _LotsListScreenState();
}

class _LotsListScreenState extends ConsumerState<LotsListScreen> {
  late LotFilters _filters;
  final _scrollController = ScrollController();
  final _searchController = TextEditingController();
  bool _searchMode = false;

  @override
  void initState() {
    super.initState();
    _filters = widget.initialFilters;
    _searchMode = (_filters.query ?? '').isNotEmpty;
    _searchController.text = _filters.query ?? '';
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      if (_searchMode) {
        return;
      }
      ref.read(lotsListProvider(_filters).notifier).loadMore();
    }
  }

  void _syncUrl(LotFilters filters) {
    final params = <String, String>{};
    if ((filters.query ?? '').isNotEmpty) params['q'] = filters.query!;
    if (filters.categoryId != null) params['categoryId'] = '${filters.categoryId}';
    if (filters.minPriceCents != null) params['minPriceCents'] = '${filters.minPriceCents}';
    if (filters.maxPriceCents != null) params['maxPriceCents'] = '${filters.maxPriceCents}';
    final uri = Uri(path: '/lots', queryParameters: params.isEmpty ? null : params);
    context.go(uri.toString());
  }

  void _onSearchChanged(String value) {
    setState(() {
      _searchMode = value.trim().isNotEmpty;
      _filters = _filters.copyWith(query: value);
    });
    _syncUrl(_filters);
    ref.read(searchProvider(_filters).notifier).updateQuery(value);
  }

  void _applyCategoryFacet(int categoryId) {
    setState(() => _filters = _filters.copyWith(categoryId: categoryId));
    _syncUrl(_filters);
    ref.read(searchProvider(_filters).notifier).updateQuery(_filters.query ?? '');
  }

  void _clearFilters() {
    setState(() {
      _filters = _filters.clearFilters();
      _searchMode = false;
      _searchController.clear();
    });
    _syncUrl(_filters);
    ref.read(searchProvider(_filters).notifier).clearFilters();
  }

  @override
  Widget build(BuildContext context) {
    if (_searchMode) {
      final searchState = ref.watch(searchProvider(_filters));
      return _buildScaffold(
        body: _buildSearchBody(searchState),
        facets: searchState.facets,
      );
    }

    final lotsAsync = ref.watch(lotsListProvider(_filters));
    return _buildScaffold(
      body: lotsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(context.l10n.lotsError),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: () => ref.read(lotsListProvider(_filters).notifier).reload(),
                child: Text(context.l10n.retry),
              ),
            ],
          ),
        ),
        data: (state) {
          if (state.items.isEmpty) {
            return Center(child: Text(context.l10n.lotsEmpty));
          }
          return _buildList(state.items, state.isLoadingMore);
        },
      ),
    );
  }

  Widget _buildScaffold({required Widget body, LotFacetsDto? facets}) {
    return Scaffold(
      appBar: AppBar(
        title: Text(context.l10n.lotsTitle),
        actions: [
          IconButton(
            icon: const Icon(Icons.filter_list),
            onPressed: () => _openFilters(context),
          ),
        ],
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(56),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: context.l10n.lotsSearchHint,
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: _clearFilters,
                      )
                    : null,
              ),
              onChanged: _onSearchChanged,
            ),
          ),
        ),
      ),
      body: Column(
        children: [
          if (facets != null) _FacetChips(facets: facets, onCategorySelected: _applyCategoryFacet),
          Expanded(child: body),
        ],
      ),
    );
  }

  Widget _buildSearchBody(SearchState searchState) {
    if (searchState.isLoading && searchState.items.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (searchState.error != null) {
      return Center(child: Text(context.l10n.lotsError));
    }
    if (searchState.items.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(context.l10n.lotsSearchEmpty(searchState.query)),
            const SizedBox(height: 12),
            FilledButton(onPressed: _clearFilters, child: Text(context.l10n.lotsClearFilters)),
          ],
        ),
      );
    }
    return _buildList(searchState.items, false);
  }

  Widget _buildList(List<LotDto> items, bool isLoadingMore) {
    return ListView.builder(
      controller: _scrollController,
      itemCount: items.length + (isLoadingMore ? 1 : 0),
      itemBuilder: (context, index) {
        if (index >= items.length) {
          return const Padding(
            padding: EdgeInsets.all(16),
            child: Center(child: CircularProgressIndicator()),
          );
        }
        return _LotTile(lot: items[index]);
      },
    );
  }

  Future<void> _openFilters(BuildContext context) async {
    final result = await showModalBottomSheet<LotFilters>(
      context: context,
      showDragHandle: true,
      builder: (context) => _FiltersSheet(initial: _filters),
    );
    if (result != null && result != _filters) {
      setState(() => _filters = result);
      _syncUrl(_filters);
    }
  }
}

class _FacetChips extends StatelessWidget {
  const _FacetChips({required this.facets, required this.onCategorySelected});

  final LotFacetsDto facets;
  final ValueChanged<int> onCategorySelected;

  @override
  Widget build(BuildContext context) {
    if (facets.categories.isEmpty) {
      return const SizedBox.shrink();
    }
    return SizedBox(
      height: 48,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        itemBuilder: (context, index) {
          final facet = facets.categories[index];
          return FilterChip(
            label: Text('${facet.name} (${facet.count})'),
            onSelected: (_) => onCategorySelected(facet.id),
          );
        },
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemCount: facets.categories.length,
      ),
    );
  }
}

class _LotTile extends StatelessWidget {
  const _LotTile({required this.lot});

  final LotDto lot;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(lot.title),
      subtitle: Text('${lot.currentPrice} · ${lot.status}'),
      trailing: lot.watched ? const Icon(Icons.bookmark) : null,
      onTap: () => context.go('/lots/${lot.id}'),
    );
  }
}

class _FiltersSheet extends StatefulWidget {
  const _FiltersSheet({required this.initial});

  final LotFilters initial;

  @override
  State<_FiltersSheet> createState() => _FiltersSheetState();
}

class _FiltersSheetState extends State<_FiltersSheet> {
  late String _status;
  late String _sort;

  @override
  void initState() {
    super.initState();
    _status = widget.initial.status;
    _sort = widget.initial.sort;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;

    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(l10n.lotsFiltersTitle, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 16),
          DropdownButtonFormField<String>(
            initialValue: _status,
            decoration: InputDecoration(labelText: l10n.lotsFilterStatus),
            items: const [
              DropdownMenuItem(value: 'LIVE', child: Text('Live')),
              DropdownMenuItem(value: 'SCHEDULED', child: Text('Scheduled')),
              DropdownMenuItem(value: 'CLOSED_SOLD', child: Text('Closed')),
            ],
            onChanged: (value) => setState(() => _status = value ?? 'LIVE'),
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _sort,
            decoration: InputDecoration(labelText: l10n.lotsFilterSort),
            items: const [
              DropdownMenuItem(value: 'endingSoon', child: Text('Ending soon')),
              DropdownMenuItem(value: 'newest', child: Text('Newest')),
              DropdownMenuItem(value: 'priceAsc', child: Text('Price ↑')),
              DropdownMenuItem(value: 'priceDesc', child: Text('Price ↓')),
            ],
            onChanged: (value) => setState(() => _sort = value ?? 'endingSoon'),
          ),
          const SizedBox(height: 24),
          FilledButton(
            onPressed: () => Navigator.pop(
              context,
              widget.initial.copyWith(status: _status, sort: _sort),
            ),
            child: Text(l10n.lotsApplyFilters),
          ),
        ],
      ),
    );
  }
}
