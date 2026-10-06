package ro.avi.minegicka3.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Minegicka;

/**
 * Server → client: draw line segments for one tick. kind 0 = beam, 1 = lightning arc, 2 = nova ring.
 * points holds segment pairs (a0, b0, a1, b1, ...); colors are the spell's element colours.
 */
public record LineFxPayload(int kind, List<Vec3> points, List<Integer> colors) implements CustomPacketPayload {
	public static final int BEAM = 0, LIGHTNING = 1, NOVA = 2;
	public static final Type<LineFxPayload> TYPE = new Type<>(Minegicka.id("line_fx"));
	public static final StreamCodec<ByteBuf, LineFxPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, LineFxPayload::kind,
		Vec3.STREAM_CODEC.apply(ByteBufCodecs.list(64)), LineFxPayload::points,
		ByteBufCodecs.INT.apply(ByteBufCodecs.list(8)), LineFxPayload::colors,
		LineFxPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
