using System;
using GoogleApi.Entities.Common.Enums;
using Newtonsoft.Json.Utilities;
using UnityEngine;

namespace CafeMap.Player.CafeMap.Miscellaneous
{
    class AotTypeEnforcer : MonoBehaviour
    {
        private void Awake()
        {
            AotHelper.EnsureList<PlaceLocationType?>();
        }
    }
}
