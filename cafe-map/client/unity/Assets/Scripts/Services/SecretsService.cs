using System.Collections.Generic;
using Google.Maps;
using Google.Protobuf;
using Org.Curioswitch.Cafemap.Api;
using UnityEngine;
using Zenject;

namespace CafeMap.Player.Services
{
    public class SecretsService
    {
        private readonly string googleApiKey;
        private readonly GetPlacesResponse placedb;

        public SecretsService([Inject(Id = "Secrets")] List<TextAsset> secrets)
        {
            foreach (var secret in secrets)
            {
                var value = secret.text.Trim();
                switch (secret.name)
                {
                    case "google-api-key":
                        googleApiKey = value;
                        break;
                    case "placedb.pb":
                        placedb = GetPlacesResponse.Parser.ParseFrom(ByteString.CopyFrom(secret.bytes));
                        break;
                }
            }
            Debug.Log(placedb.ToString());
        }

        public string GoogleApiKey => googleApiKey;
        public GetPlacesResponse PlaceDb => placedb;

        [Inject]
        public void Init(MapsService mapsService)
        {
            mapsService.ApiKey = googleApiKey;
        }
    }
}