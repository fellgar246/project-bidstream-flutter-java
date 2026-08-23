import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:permission_handler/permission_handler.dart';

import '../../../core/l10n/locale_provider.dart';
import '../data/lot_dto.dart';
import '../data/lot_image_dto.dart';
import '../providers/image_upload_provider.dart';
import '../providers/image_upload_state.dart';
import '../providers/lot_detail_provider.dart';
import '../providers/lot_images_api_provider.dart';

class LotEditScreen extends ConsumerStatefulWidget {
  const LotEditScreen({super.key, required this.lotId});

  final int lotId;

  @override
  ConsumerState<LotEditScreen> createState() => _LotEditScreenState();
}

class _LotEditScreenState extends ConsumerState<LotEditScreen> {
  final _picker = ImagePicker();
  bool _permissionDenied = false;

  @override
  Widget build(BuildContext context) {
    final lotAsync = ref.watch(lotDetailProvider(widget.lotId));
    final uploads = ref.watch(imageUploadProvider(widget.lotId));
    final l10n = context.l10n;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.lotEditTitle)),
      body: lotAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, _) => Center(child: Text(l10n.lotsError)),
        data: (detail) {
          final lot = detail.lot;
          return ListView(
            padding: const EdgeInsets.all(24),
            children: [
              Text(lot.title, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 16),
              Text(
                l10n.lotImagesTitle,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 12),
              if (_permissionDenied) ...[
                Text(l10n.lotCameraPermissionDenied),
                const SizedBox(height: 8),
                OutlinedButton(
                  onPressed: openAppSettings,
                  child: Text(l10n.lotOpenSettings),
                ),
                const SizedBox(height: 12),
              ],
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final image in lot.images)
                    _ExistingImageTile(image: image, lotId: widget.lotId),
                  for (final upload in uploads)
                    _UploadTile(lotId: widget.lotId, item: upload),
                  _AddImageButton(
                    onGallery: () => _pick(ImageSource.gallery),
                    onCamera: () => _pick(ImageSource.camera),
                  ),
                ],
              ),
              if (lot.images.length > 1) ...[
                const SizedBox(height: 24),
                Text(l10n.lotReorderHint),
                const SizedBox(height: 8),
                ReorderableListView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: lot.images.length,
                  onReorderItem: (oldIndex, newIndex) =>
                      _reorderImages(context, lot, oldIndex, newIndex),
                  itemBuilder: (context, index) {
                    final image = lot.images[index];
                    return ListTile(
                      key: ValueKey(image.id),
                      leading: const Icon(Icons.drag_handle),
                      title: Text(
                        '${l10n.lotImagePosition} ${image.position + 1}',
                      ),
                      subtitle: Text(image.contentType),
                    );
                  },
                ),
              ],
            ],
          );
        },
      ),
    );
  }

  Future<void> _pick(ImageSource source) async {
    if (source == ImageSource.camera) {
      final status = await Permission.camera.request();
      if (!status.isGranted) {
        setState(() => _permissionDenied = true);
        return;
      }
      setState(() => _permissionDenied = false);
    }

    final picked = await _picker.pickImage(source: source, imageQuality: 85);
    if (picked == null) {
      return;
    }

    final compressed = await FlutterImageCompress.compressWithFile(
      picked.path,
      minWidth: 1600,
      minHeight: 1600,
      quality: 80,
      format: CompressFormat.jpeg,
    );
    final bytes = Uint8List.fromList(compressed ?? await picked.readAsBytes());
    ref
        .read(imageUploadProvider(widget.lotId).notifier)
        .addSelected(
          localId: picked.path,
          bytes: bytes,
          fileName: picked.name,
          contentType: 'image/jpeg',
        );
  }

  Future<void> _reorderImages(
    BuildContext context,
    LotDto lot,
    int oldIndex,
    int newIndex,
  ) async {
    final images = List<LotImageDto>.from(lot.images);
    final moved = images.removeAt(oldIndex);
    images.insert(newIndex, moved);
    final previous = lot.images.map((image) => image.id).toList();
    final optimistic = images.map((image) => image.id).toList();

    ref
        .read(lotDetailProvider(widget.lotId).notifier)
        .applyOptimisticImages(images);

    try {
      await ref
          .read(lotImagesApiProvider)
          .reorder(lotId: widget.lotId, order: optimistic);
      await ref.read(lotDetailProvider(widget.lotId).notifier).reload();
    } catch (_) {
      ref
          .read(lotDetailProvider(widget.lotId).notifier)
          .applyOptimisticOrder(previous);
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(context.l10n.lotReorderFailed)));
      }
    }
  }
}

class _AddImageButton extends StatelessWidget {
  const _AddImageButton({required this.onGallery, required this.onCamera});

  final VoidCallback onGallery;
  final VoidCallback onCamera;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return PopupMenuButton<String>(
      onSelected: (value) => value == 'camera' ? onCamera() : onGallery(),
      itemBuilder: (context) => [
        PopupMenuItem(value: 'gallery', child: Text(l10n.lotPickGallery)),
        PopupMenuItem(value: 'camera', child: Text(l10n.lotPickCamera)),
      ],
      child: Container(
        width: 96,
        height: 96,
        decoration: BoxDecoration(
          border: Border.all(color: Theme.of(context).colorScheme.outline),
          borderRadius: BorderRadius.circular(12),
        ),
        child: const Icon(Icons.add_a_photo_outlined),
      ),
    );
  }
}

class _UploadTile extends ConsumerWidget {
  const _UploadTile({required this.lotId, required this.item});

  final int lotId;
  final ImageUploadItem item;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = context.l10n;
    return Container(
      width: 96,
      height: 96,
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
      ),
      child: switch (item.phase) {
        ImageUploadPhase.uploading => Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(value: item.progress),
            const SizedBox(height: 4),
            Text(l10n.lotUploading),
          ],
        ),
        ImageUploadPhase.confirming => Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const CircularProgressIndicator(),
            const SizedBox(height: 4),
            Text(l10n.lotConfirming),
          ],
        ),
        ImageUploadPhase.error => Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline),
            TextButton(
              onPressed: () => ref
                  .read(imageUploadProvider(lotId).notifier)
                  .retry(item.localId),
              child: Text(l10n.retry),
            ),
          ],
        ),
        ImageUploadPhase.ready => const Icon(Icons.check_circle_outline),
        ImageUploadPhase.selected => const CircularProgressIndicator(),
      },
    );
  }
}

class _ExistingImageTile extends ConsumerWidget {
  const _ExistingImageTile({required this.image, required this.lotId});

  final LotImageDto image;
  final int lotId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final displayUrl = image.thumbnailUrl ?? image.url;
    return Stack(
      children: [
        Container(
          width: 96,
          height: 96,
          clipBehavior: Clip.hardEdge,
          decoration: BoxDecoration(borderRadius: BorderRadius.circular(12)),
          child: displayUrl == null
              ? const ColoredBox(color: Colors.black12)
              : Image.network(displayUrl, fit: BoxFit.cover),
        ),
        Positioned(
          top: 0,
          right: 0,
          child: IconButton(
            iconSize: 18,
            visualDensity: VisualDensity.compact,
            onPressed: () async {
              await ref
                  .read(lotImagesApiProvider)
                  .deleteImage(lotId: lotId, imageId: image.id);
              await ref.read(lotDetailProvider(lotId).notifier).reload();
            },
            icon: const Icon(Icons.close),
          ),
        ),
      ],
    );
  }
}
