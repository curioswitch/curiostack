// Un-textured Color Shader.
// Diffuse and specular lighting with fog and shadows (casting and receiving).
// This extension is also compatible with LOD-driven fading, allowing buildings
// using this shader to be faded out smoothly by LOD Groups.
Shader "Google/Maps/Shaders/Color with Specular, Lod" {
  Properties {
    // Regular (diffuse) color.
    _Color("Color", Color) = (1.0, 1.0, 1.0, 1.0)

    // Shininess (gloss) value.
    _Shininess("Specular Shininess", Float) = 10.0
  }

  SubShader {
    Pass {
      // Transparency needed for rendering LOD crossfades.
      Tags {
        "Queue" = "Transparent"
        "LightMode" = "ForwardBase"
      }

      Cull Back // Cull backfaces.
      ZWrite On // Use Z-buffer.
      ZTest LEqual // Use normal (less-equal) z-depth check.

      // Use traditional transparency (needed for LOD crossfades).
      Blend SrcAlpha OneMinusSrcAlpha

      CGPROGRAM
      #pragma vertex vert // Vertex shader.
      #pragma fragment frag // Fragment shader.
      #pragma multi_compile_fog // To make fog work.

      #include "UnityCG.cginc" // Standard unity helper functions.
      #include "Lighting.cginc" // Lighting functions.
      #include "AutoLight.cginc" // Shadow functions and macros.

      // Compile two versions, one with and one without lod cross fading.
      #pragma multi_compile _ LOD_FADE_CROSSFADE

      // User defined values.
      uniform float _Shininess;
      uniform float4 _Color;

      // Vertex Shader input.
      struct vertexInput {
        float4 vertex : POSITION; // Vertex worldspace position.
        float3 normal : NORMAL; // Vertex normal vector.
      };

      // Fragment Shader Input.
      struct vertexOutput {
        float4 pos : SV_POSITION; // Vertex screenspace position.
        float3 normal : NORMAL; // Vertex normal vector.
        float4 vertex : TEXCOORD0; // Vertex worldspace position.
        SHADOW_COORDS(1) // Shadow data as TEXCOORD3.
        UNITY_FOG_COORDS(2) // Per-vertex fog as TEXCOORD4.
      };

      // Vertex Shader.
      // Note: MUST be 'v' for TRANSFER_SHADOW to work.
      vertexOutput vert(vertexInput v) {
        // Computer screenspace position.
        vertexOutput output;
        output.pos = UnityObjectToClipPos(v.vertex);

        // Get vertex worldspace position and normals for
        // lighting calculation in fragment shader.
        float4x4 modelMatrix = unity_ObjectToWorld;
        float4x4 modelMatrixInverse = unity_WorldToObject;

        output.vertex = mul(modelMatrix, v.vertex);
        output.normal
            = normalize(mul(float4(v.normal, 0.0), modelMatrixInverse).xyz);

        // Compute shadows data (requires input to be 'v').
        TRANSFER_SHADOW(output)

        // Apply fog.
        UNITY_TRANSFER_FOG(output, output.pos);
        return output;
      }

      // Fragment Shader.
      fixed4 frag(vertexOutput input) : SV_Target {
        // Start with colors.
        fixed3 diffColor = _Color;
        fixed3 specColor = fixed3(1.0, 1.0, 1.0);

        // Calculate shadow value.
        fixed shadow = SHADOW_ATTENUATION(input);

        // Calculate lighting.
        float3 normalDirection = normalize(input.normal);
        float3 viewDirection
            = normalize(_WorldSpaceCameraPos - input.vertex.xyz);
        float3 lightDirection;
        float attenuation;

        // If light is directional, have no attenuation (no falloff).
        if (0.0 == _WorldSpaceLightPos0.w) {
          attenuation = 1.0;
          lightDirection = normalize(_WorldSpaceLightPos0.xyz);
        } else { // For point or spot light, use linear attenuation.
          float3 vertexToLightSource
              = _WorldSpaceLightPos0.xyz - input.vertex.xyz;
          float distance = length(vertexToLightSource);
          attenuation = 1.0 / distance;
          lightDirection = normalize(vertexToLightSource);
        }

        // Calculate ambient and diffuse lighting.
        float3 ambientLighting = UNITY_LIGHTMODEL_AMBIENT.rgb * diffColor;

        float3 diffuseReflection
            = shadow * attenuation * _LightColor0.rgb * diffColor
            * max(0.0, dot(normalDirection, lightDirection));

        // If light source is on the wrong side, have no specular light.
        float3 specularReflection;
        if (dot(normalDirection, lightDirection) < 0.0) {
           specularReflection = float3(0.0, 0.0, 0.0);
        } else {
          specularReflection = shadow * attenuation * _LightColor0.rgb
              * specColor * pow(max(0.0, dot(reflect(-lightDirection,
              normalDirection), viewDirection)), _Shininess);
        }

        // Collection lighting together.
        fixed4 finalColor = fixed4(ambientLighting + diffuseReflection
            + specularReflection, 1.0);

        // Apply fog.
        UNITY_APPLY_FOG(input.fogCoord, finalColor);

        // Optionally apply lod.
        #ifdef LOD_FADE_CROSSFADE
            finalColor.a *= unity_LODFade.x;
        #endif
        return finalColor;
      }
      ENDCG
    }

    // Pull in shadow caster pass from VertexLit built in-shader.
    // Remove to make this receive shadows only (but not cast them).
    UsePass "Legacy Shaders/VertexLit/SHADOWCASTER"
  }

  // Fallback to default diffuse textured shader.
  Fallback "Specular"
}
