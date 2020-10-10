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
        private readonly Dictionary<string, Texture2D> _photoCache = new Dictionary<string, Texture2D>();

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

        public async UniTask<Texture2D> getPhoto(string placeId)
        {
            Texture2D photoTexture;
            if (_photoCache.TryGetValue(placeId, out photoTexture))
            {
                return photoTexture;
            }
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

                photoTexture = DownloadHandlerTexture.GetContent(uwr);
            }
            _photoCache[placeId] = photoTexture;
            return photoTexture;
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
