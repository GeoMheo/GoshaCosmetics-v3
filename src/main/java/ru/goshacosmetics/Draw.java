package ru.goshacosmetics;

import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

/** Двусторонние цветные четырёхугольники (для слоя свечения). */
final class Draw {
    private Draw() {}

    static void quad(VertexConsumer vc, Matrix4f m,
                     float ax, float ay, float az, int ca,
                     float bx, float by, float bz, int cb,
                     float cx, float cy, float cz, int cc,
                     float dx, float dy, float dz, int cd) {
        vc.vertex(m, ax, ay, az).color(ca);
        vc.vertex(m, bx, by, bz).color(cb);
        vc.vertex(m, cx, cy, cz).color(cc);
        vc.vertex(m, dx, dy, dz).color(cd);
        vc.vertex(m, dx, dy, dz).color(cd);
        vc.vertex(m, cx, cy, cz).color(cc);
        vc.vertex(m, bx, by, bz).color(cb);
        vc.vertex(m, ax, ay, az).color(ca);
    }
}
