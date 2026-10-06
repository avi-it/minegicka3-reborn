package ro.avi.minegicka3.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ro.avi.minegicka3.Minegicka;

/**
 * Client → server. action: 0 = start spell (elements + cast type), 1 = stop the running spell,
 * 2 = cast magick (elements), 3 = weapon imbue (elements).
 */
public record CastPayload(int action, byte[] elements, int castType) implements CustomPacketPayload {
	public static final int START = 0, STOP = 1, MAGICK = 2, IMBUE = 3;
	public static final Type<CastPayload> TYPE = new Type<>(Minegicka.id("cast"));
	public static final StreamCodec<ByteBuf, CastPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, CastPayload::action,
		ByteBufCodecs.BYTE_ARRAY, CastPayload::elements,
		ByteBufCodecs.VAR_INT, CastPayload::castType,
		CastPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
