using System.Collections.Generic;
using Google.Maps;
using UnityEngine;
using Zenject;

namespace CafeMap.Player.Services
{
    public class KeysService
    {
        private readonly string googleApiKey;

        public KeysService([Inject(Id = "Secrets")] List<TextAsset> secrets)
        {
            foreach (var secret in secrets)
            {
                var value = secret.text.Trim();
                switch (secret.name)
                {
                    case "google-api-key":
                        googleApiKey = value;
                        break;
                }
            }
        }

        public string GoogleApiKey => googleApiKey;

        [Inject]
        public void Init(MapsService mapsService)
        {
            mapsService.ApiKey = googleApiKey;
        }
    }
}