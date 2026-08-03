import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/l10n/app_strings.dart';
import '../data/lot_dto.dart';
import '../data/lot_filters.dart';
import '../providers/lots_list_provider.dart';

class LotsListScreen extends ConsumerStatefulWidget {
  const LotsListScreen({super.key});

  @override
  ConsumerState<LotsListScreen> createState() => _LotsListScreenState();
}

class _LotsListScreenState extends ConsumerState<LotsListScreen> {
  LotFilters _filters = const LotFilters();
  final _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      ref.read(lotsListProvider(_filters).notifier).loadMore();
    }
  }

  @override
  Widget build(BuildContext context) {
    final lotsAsync = ref.watch(lotsListProvider(_filters));

    return Scaffold(
      appBar: AppBar(
        title: const Text(AppStrings.lotsTitle),
        actions: [
          IconButton(
            icon: const Icon(Icons.filter_list),
            onPressed: () => _openFilters(context),
          ),
        ],
      ),
      body: lotsAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(AppStrings.lotsError),
              const SizedBox(height: 12),
              FilledButton(
                onPressed: () => ref.read(lotsListProvider(_filters).notifier).reload(),
                child: const Text(AppStrings.retry),
              ),
            ],
          ),
        ),
        data: (state) {
          if (state.items.isEmpty) {
            return const Center(child: Text(AppStrings.lotsEmpty));
          }
          return ListView.builder(
            controller: _scrollController,
            itemCount: state.items.length + (state.isLoadingMore ? 1 : 0),
            itemBuilder: (context, index) {
              if (index >= state.items.length) {
                return const Padding(
                  padding: EdgeInsets.all(16),
                  child: Center(child: CircularProgressIndicator()),
                );
              }
              final lot = state.items[index];
              return _LotTile(lot: lot);
            },
          );
        },
      ),
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
    }
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
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(AppStrings.lotsFiltersTitle, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 16),
          DropdownButtonFormField<String>(
            initialValue: _status,
            decoration: const InputDecoration(labelText: AppStrings.lotsFilterStatus),
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
            decoration: const InputDecoration(labelText: AppStrings.lotsFilterSort),
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
            child: const Text(AppStrings.lotsApplyFilters),
          ),
        ],
      ),
    );
  }
}
