#version 330
#extension GL_ARB_separate_shader_objects : require

// Thermal view, white hot. A post shader only sees colours, so heat is faked: bright and warm
// colours (fire, lava, skin, lamps) read hot, the sky and water cold, and outlines are drawn
// where the brightness changes sharply, like the original's "pencil" shader.

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(location = 0) out vec4 fragColor;

const vec3 Gray = vec3(0.3, 0.59, 0.11);

float heat(vec3 c) {
    float luma = dot(c, Gray);
    float warmth = clamp(c.r - c.b, 0.0, 1.0);
    return clamp(luma * 0.7 + warmth * 0.6, 0.0, 1.0);
}

void main() {
    vec2 oneTexel = 1.0 / InSize;
    float c = heat(texture(InSampler, texCoord).rgb);
    float l = heat(texture(InSampler, texCoord - vec2(oneTexel.x, 0.0)).rgb);
    float r = heat(texture(InSampler, texCoord + vec2(oneTexel.x, 0.0)).rgb);
    float u = heat(texture(InSampler, texCoord - vec2(0.0, oneTexel.y)).rgb);
    float d = heat(texture(InSampler, texCoord + vec2(0.0, oneTexel.y)).rgb);
    float edge = clamp(abs(r - l) + abs(d - u), 0.0, 1.0);

    float value = smoothstep(0.08, 0.85, c) + edge * 0.6;
    fragColor = vec4(vec3(clamp(value, 0.0, 1.0)), 1.0);
}
