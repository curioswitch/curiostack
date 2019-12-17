// Unity built-in shader source. Copyright (c) 2016 Unity Technologies.
// MIT license (see license.txt).
// Modified version to overlay this UI element over all other worldspace
// and UI elements, regardless of position.
Shader "Google/Maps/Shaders/UI Overlay" {
  Properties {
    [PerRendererData] _MainTex ("Sprite Texture", 2D) = "white" {}
    _Color ("Tint", Color) = (1,1,1,1)

    _StencilComp ("Stencil Comparison", Float) = 8
    _Stencil ("Stencil ID", Float) = 0
    _StencilOp ("Stencil Operation", Float) = 0
    _StencilWriteMask ("Stencil Write Mask", Float) = 255
    _StencilReadMask ("Stencil Read Mask", Float) = 255

    _ColorMask ("Color Mask", Float) = 15

    [Toggle(UNITY_UI_ALPHACLIP)] _UseUIAlphaClip ("Use Alpha Clip", Float) = 0
  } SubShader {
    Tags {
      "Queue" = "Transparent"
      "IgnoreProjector" = "True"
      "RenderType" = "Transparent"
      "PreviewType" = "Plane"
      "CanUseSpriteAtlas" = "True"
    }

    Stencil {
      Ref [_Stencil]
      Comp [_StencilComp]
      Pass [_StencilOp]
      ReadMask [_StencilReadMask]
      WriteMask [_StencilWriteMask]
    }
    Cull Off
    Lighting Off
    Blend SrcAlpha OneMinusSrcAlpha
    ColorMask [_ColorMask]

    // This is the only substantive change made to this standard UI shader -
    // disabling z-buffer and making sure this element is always rendered (even
    // if it is behind another element).
    ZWrite Off
    ZTest Always

    Pass {
      Name "Default"
      CGPROGRAM
      #pragma vertex vert
      #pragma fragment frag
      #pragma target 2.0

      #include "UnityCG.cginc"
      #include "UnityUI.cginc"

      #pragma multi_compile __ UNITY_UI_ALPHACLIP

      struct appdata_t {
        float4 vertex : POSITION;
        float4 color : COLOR;
        float2 texcoord : TEXCOORD0;
        UNITY_VERTEX_INPUT_INSTANCE_ID
      };

      struct v2f {
        float4 vertex : SV_POSITION;
        fixed4 color : COLOR;
        float2 texcoord : TEXCOORD0;
        float4 worldPosition : TEXCOORD1;
        UNITY_VERTEX_OUTPUT_STEREO
      };

      fixed4 _Color;
      fixed4 _TextureSampleAdd;
      float4 _ClipRect;

      v2f vert (appdata_t IN) {
        v2f OUT;
        UNITY_SETUP_INSTANCE_ID(IN);
        UNITY_INITIALIZE_VERTEX_OUTPUT_STEREO(OUT);
        OUT.worldPosition = IN.vertex;
        OUT.vertex = UnityObjectToClipPos(OUT.worldPosition);

        OUT.texcoord = IN.texcoord;

        OUT.color = IN.color * _Color;
        return OUT;
      }

      sampler2D _MainTex;

      fixed4 frag (v2f IN) : SV_Target {
        half4 color =
            (tex2D(_MainTex, IN.texcoord) + _TextureSampleAdd) * IN.color;

        color.a *= UnityGet2DClipping(IN.worldPosition.xy, _ClipRect);

        #ifdef UNITY_UI_ALPHACLIP
        clip (color.a - 0.001);
        #endif

        return color;
      }
      ENDCG
    }
  }
}
