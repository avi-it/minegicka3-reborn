package ro.avi.minegicka3.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import ro.avi.minegicka3.Minegicka;

/** Server → client: a spray droplet was emitted; the client simulates the same motion and draws it. */
public record SprayFxPayload(Vec3 pos, Vec3 vel, Vec3 axis, int element, int count) implements CustomPacketPayload {
	public static final Type<SprayFxPayload> TYPE = new Type<>(Minegicka.id("spray_fx"));
	public static final StreamCodec<ByteBuf, SprayFxPayload> CODEC = StreamCodec.composite(
		Vec3.STREAM_CODEC, SprayFxPayload::pos,
		Vec3.STREAM_CODEC, SprayFxPayload::vel,
		Vec3.STREAM_CODEC, SprayFxPayload::axis,
		ByteBufCodecs.VAR_INT, SprayFxPayload::element,
		ByteBufCodecs.VAR_INT, SprayFxPayload::count,
		SprayFxPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
