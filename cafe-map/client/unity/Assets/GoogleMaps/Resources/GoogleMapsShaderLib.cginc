#ifndef NINE_SLICING
#define NINE_SLICING

// Returns one of two float values depending on the value of the provided condition variable. This
// is provided as an alternative to if/else branching, which is inefficient on GPUs.
// condition: The condition to evaluate.
// ifTrue: Returned if cond is true.
// ifFalse: Returned if cond is false.
float _ifElsef(bool condition, float ifTrue, float ifFalse) {
  return (ifTrue * (condition > 0)) + (ifFalse * (condition == 0));
}

// A struct containing the texture coordinate information of a nine-sliced fragment.
struct nineSliceResult {
  // A UV coordinates for a texture.
  float2 uv;

  // A partial derivative of UV coordinates respective to the screen-space x-coordinate.
  // The derivative represents the rate of changes between texture coordinates in two adjacent
  // fragments, and is used for passing into the `tex2D` function for selecting the the correct
  // mipmap level of the texture.
  float2 ddx;

  // A partial derivative of UV coordinates respective to the screen-space y-coordinate.
  // The derivative represents the rate of changes between texture coordinates in two adjacent
  // fragments, and is used for passing into the `tex2D` function for selecting the the correct
  // mipmap level of the texture.
  float2 ddy;
};

// Nine-slices the UV coordinates of a fragment within the specified bounds. Nine-slicing is used to
// tile the applied texture in the following manner:
//
//             +            +
//     (0, 1)  |            |  (1, 1)
//        +----------------------+
//        |    |   tiled    |    |
//        |    |     x      |    |    upper
//     +----------------------------+ bound
//        | t  |            | t  |
//        | i  |   tiled    | i  |
//        | l y|   x & y    | l y|
//        | e  |            | e  |
//        | d  |            | d  |
//     +----------------------------+ lower
//        |    |   tiled    |    |    bound
//        |    |     x      |    |
//        +----------------------+
//     (0, 0)  |            |  (1, 0)
//             +            +
//           left         right
//           bound        bound
//
// It offers functionality similar to Unity's inbuilt nine-slicing for sprites:
// https://docs.unity3d.com/Manual/9SliceSprites.html.
// pos: The (x, y) coordinates of the fragment being textured, relative to the origin of the face.
// faceSize: The (width, height) of the face being nine-sliced.
// texSize: The (width, height) of the base texture.
// leftBound: The position of the left nine-slicing boundary normalized to the width of the texture
//     (in the range 0 to 1 inclusive). Tiling on the U axis only occurs to the right of leftBound.
// rightBound: The position of the right nine-slicing boundary normalized to the width of the
//     texture (in the range 0 to 1 inclusive). Tiling on the U axis only occurs to the left of
//     rightBound.
// lowerBound: The position of the lower nine-slicing boundary normalized to the height of the
//     texture (in the range 0 to 1 inclusive). Tiling on the V axis only occurs above lowerBound.
// upperBound: The position of the upper nine-slicing boundary normalized to the height of the
//     texture (in the range 0 to 1 inclusive). Tiling on the V axis only occurs below upperBound.
nineSliceResult nineSlice(
    float2 pos, float2 faceSize, float2 texSize,
    float leftBound, float rightBound, float lowerBound, float upperBound) {
  // Normalize relative to texture size.
  float2 normalFaceSize = faceSize / texSize;
  float2 normalPos = pos / texSize;

  // Calculate the size of the center fragment of the face.
  float centerFaceSizeX = normalFaceSize.x - leftBound - (1 - rightBound);
  float centerFaceSizeY = normalFaceSize.y - lowerBound - (1 - upperBound);

  // Calculate the size of the center fragment of each tiling;
  float centerTexSizeX = rightBound - leftBound;
  float centerTexSizeY = upperBound - lowerBound;

  // Calculate the number of tilings in the center fragment of the face.
  int tileNumX = max(round(centerFaceSizeX / centerTexSizeX), 1);
  int tileNumY = max(round(centerFaceSizeY / centerTexSizeY), 1);

  // Calculate the continuous UV used for calculating the derivative of the UV.
  float2 continuousUV = float2(
      pos.x / faceSize.x * (centerTexSizeX * tileNumX + leftBound + (1 - rightBound)),
      pos.y / faceSize.y * (centerTexSizeY * tileNumY + lowerBound + (1 - upperBound)));

  // Calculate bounding conditionals.
  bool isLeftBound = normalPos.x < leftBound;
  bool isRightBound = normalPos.x > normalFaceSize.x - (1 - rightBound);
  bool isLowerBound = normalPos.y < lowerBound;
  bool isUpperBound = normalPos.y > normalFaceSize.y - (1 - upperBound);

  // Calculate UV coordinates within each bound.
  float leftBoundU = normalPos.x;
  float rightBoundU = 1 - (normalFaceSize.x - normalPos.x);
  float centerBoundU = lerp(leftBound, rightBound,
      (normalPos.x - leftBound) * tileNumX / centerFaceSizeX % 1);

  float lowerBoundV = normalPos.y;
  float upperBoundV = 1 - (normalFaceSize.y - normalPos.y);
  float centerBoundV = lerp(lowerBound, upperBound,
      (normalPos.y - lowerBound) * tileNumY / centerFaceSizeY % 1);

  // Construct final UV coordinates based on the fragment's location in the face.
  float u = _ifElsef(isLeftBound,
    leftBoundU,
  _ifElsef(isRightBound,
    rightBoundU,
  centerBoundU));

  float v = _ifElsef(isLowerBound,
    lowerBoundV,
  _ifElsef(isUpperBound,
    upperBoundV,
  centerBoundV));

  // Construct the nine-slicing result struct.
  nineSliceResult result;
  result.uv = float2(u, v);
  result.ddx = ddx(continuousUV);
  result.ddy = ddy(continuousUV);
  return result;
}

#endif
