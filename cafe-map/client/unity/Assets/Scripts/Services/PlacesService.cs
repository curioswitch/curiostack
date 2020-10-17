using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using CafeMap.Player.Services;
using Cysharp.Threading.Tasks;
using GoogleApi;
using GoogleApi.Entities.Places.Details.Request;
using GoogleApi.Entities.Places.Details.Request.Enums;
using GoogleApi.Entities.Places.Details.Response;
using GoogleApi.Entities.Places.Photos.Request;
using UnityEngine;
using UnityEngine.Networking;
using Zenject;

namespace CafeMap.Services
{
    public class PlacesService
    {
        private readonly SecretsService _secretsService;

        private readonly Dictionary<string, DetailsResult> _detailsCache = new Dictionary<string, DetailsResult>();
        private readonly Dictionary<string, Sprite> _photoCache = new Dictionary<string, Sprite>();

        [Inject]
        public PlacesService(SecretsService secretsService)
        {
            _secretsService = secretsService;
        }

        public async Task<string> refreshPlaceId(string placeId)
        {
            var request = new PlacesDetailsRequest
            {
                Key = _secretsService.GoogleApiKey,
                PlaceId = placeId,
                Fields = FieldTypes.Place_Id,
            };

            var response = await GooglePlaces.Details.QueryAsync(request);
            return response.Result.PlaceId;
        }

        public async UniTask<Sprite> getPhoto(string placeId)
        {
            Sprite photoSprite;
            if (_photoCache.TryGetValue(placeId, out photoSprite))
            {
                Debug.Log("Cached sprite");
                return photoSprite;
            }
            Debug.Log("fetch sprite");
            var details = await getDetails(placeId);
            var photo = details.Photos.FirstOrDefault();
            if (photo == null)
            {
                return null;
            }

            var request = new PlacesPhotosRequest
            {
                Key = _secretsService.GoogleApiKey,
                PhotoReference = photo.PhotoReference,
                MaxWidth = 500,
                MaxHeight = 500,
            };

            using (var uwr = UnityWebRequestTexture.GetTexture(request.GetUri()))
            {
                await uwr.SendWebRequest();
                if (uwr.isNetworkError || uwr.isHttpError)
                {
                    return null;
                }

                var texture = DownloadHandlerTexture.GetContent(uwr);
                photoSprite = Sprite.Create(texture, new Rect(0, 0, texture.width, texture.height), Vector2.zero);
            }
            _photoCache[placeId] = photoSprite;
            return photoSprite;
        }

        public async UniTask<DetailsResult> getDetails(string placeId)
        {
            DetailsResult details;
            if (_detailsCache.TryGetValue(placeId, out details))
            {
                return details;
            }

            var request = new PlacesDetailsRequest
            {
                Key = _secretsService.GoogleApiKey,
                PlaceId = placeId,
                Fields = FieldTypes.Basic,
            };

            var response = await GooglePlaces.Details.QueryAsync(request);
            details = response.Result;
            _detailsCache[placeId] = details;
            return details;
        }
    }
}
