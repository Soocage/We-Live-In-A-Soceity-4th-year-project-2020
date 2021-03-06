#version 330 core

in vec2 position;
in vec3 color;
in vec2 textureCoords;

out vec3 passColour;
out vec2 passTextureCoords;
out vec4 passColourOffset;

uniform vec3 colourOffset;

float offsetAlpha = 1;

void main() {
    gl_Position = vec4(position, 0, 1);
    passColour = color;
    passColourOffset = vec4(colourOffset, offsetAlpha);
    passTextureCoords = textureCoords;
}