using CafeMap.Map;
using CafeMap.Player.Services;
using Google.Maps;
using UnityEngine;
using UnityEngine.UI;
using Zenject;

public class AppInstaller : MonoInstaller
{
    public override void InstallBindings()
    {
        var mapBase = GameObject.FindWithTag("GameController");
        var searchBox = GameObject.FindWithTag("SearchBox");

        Container.BindInstance(mapBase.GetComponent<MapsService>());
        Container.BindInstance(mapBase.GetComponent<DynamicMapsUpdater>());
        Container.Bind<InputField>().WithId("SearchBox").FromInstance(searchBox.GetComponent<InputField>());
        Container.BindInstance(Camera.main.GetComponent<PanAndZoom>());

        Container.Bind<TextAsset>().WithId("Secrets").FromResources("Secrets").AsSingle();

        Container.BindInterfacesAndSelfTo<KeysService>().AsSingle().NonLazy();
        Container.BindInterfacesAndSelfTo<SearchService>().AsSingle().NonLazy();
    }
}