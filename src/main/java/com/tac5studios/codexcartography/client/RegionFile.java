package com.tac5studios.codexcartography.client;

import com.tac5studios.codexcartography.CodexCartography;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Our region file: r.&lt;rx&gt;.&lt;rz&gt;.ccr, one per 512 x 512 blocks.
 *
 * <p>Layout: magic "CCR1", format version, body length, then the deflate-compressed body:
 * biome ids, then each mapped chunk (slot, scan time, colors, heights, water, light, biomes).
 * Written to a .tmp file and swapped in, so a crash mid-write never breaks the old file.
 */
public final class RegionFile {

    private static final int MAGIC = 0x43435231; // "CCR1"
    private static final int VERSION = 1;

    private RegionFile() {}

    public static Path path(Path dir, int rx, int rz) {
        return dir.resolve("r." + rx + "." + rz + ".ccr");
    }

    public static void write(Path dir, MapRegion.Snapshot snap) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new DeflaterOutputStream(bytes))) {
            out.writeInt(snap.biomes().size());
            for (String id : snap.biomes()) out.writeUTF(id);
            int count = 0;
            for (ChunkColumns c : snap.chunks()) if (c != null) count++;
            out.writeInt(count);
            for (int slot = 0; slot < snap.chunks().length; slot++) {
                ChunkColumns c = snap.chunks()[slot];
                if (c == null) continue;
                out.writeShort(slot);
                out.writeLong(c.scannedAt());
                for (int v : c.color()) out.writeInt(v);
                for (short v : c.height()) out.writeShort(v);
                out.write(c.water());
                out.write(c.light());
                for (short v : c.biome()) out.writeShort(v);
            }
        }
        byte[] body = bytes.toByteArray();

        Files.createDirectories(dir);
        Path file = path(dir, snap.rx(), snap.rz());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(body.length);
            out.write(body);
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Reads a region, or returns an empty one when there's no file. A broken file is kept as .bad and a fresh region starts. */
    public static MapRegion read(Path dir, int rx, int rz) {
        MapRegion region = new MapRegion(rx, rz);
        Path file = path(dir, rx, rz);
        if (!Files.exists(file)) return region;
        try {
            byte[] body;
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                if (in.readInt() != MAGIC) throw new IOException("not a map region file");
                int version = in.readInt();
                if (version > VERSION) throw new IOException("made by a newer version (format " + version + ")");
                body = in.readNBytes(in.readInt());
            }
            try (DataInputStream in = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(body)))) {
                int biomeCount = in.readInt();
                List<String> biomes = new ArrayList<>(biomeCount);
                for (int i = 0; i < biomeCount; i++) biomes.add(in.readUTF());
                int count = in.readInt();
                for (int n = 0; n < count; n++) {
                    int slot = in.readUnsignedShort();
                    ChunkColumns c = ChunkColumns.blank(in.readLong());
                    for (int i = 0; i < ChunkColumns.COLUMNS; i++) c.color()[i] = in.readInt();
                    for (int i = 0; i < ChunkColumns.COLUMNS; i++) c.height()[i] = in.readShort();
                    in.readFully(c.water());
                    in.readFully(c.light());
                    for (int i = 0; i < ChunkColumns.COLUMNS; i++) c.biome()[i] = in.readShort();
                    region.load(biomes, slot, c);
                }
            }
            return region;
        } catch (IOException e) {
            CodexCartography.LOGGER.warn("[Cartography] Map region {} can't be read ({}); starting it fresh.", file, e.getMessage());
            try {
                Files.move(file, file.resolveSibling(file.getFileName() + ".bad"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // the fresh region will overwrite it on the next save
            }
            return new MapRegion(rx, rz);
        }
    }
}
