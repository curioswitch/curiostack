using CafeMap.Events;
using CafeMap.Map;
using CafeMap.Player.Services;
using CafeMap.Services;
using Google.Maps;
using UnityEngine;
using UnityEngine.UI;
using Zenject;

public class AppInstaller : MonoInstaller
{
    public override void InstallBindings()
    {
        SignalBusInstaller.Install(Container);
        Container.DeclareSignal<MapOriginChanged>();
        Container.DeclareSignal<PlaceSelected>();

        var searchBox = GameObject.FindWithTag("SearchBox");
        var worldCanvas = GameObject.FindWithTag("WorldCanvas");

        Container.Bind<MapsService>().FromComponentOnRoot().AsSingle();
        Container.BindInterfacesAndSelfTo<BaseMapLoader>().FromComponentOnRoot().AsSingle();
        Container.BindInterfacesAndSelfTo<DynamicMapsUpdater>().FromComponentOnRoot().AsSingle();
        Container.BindInterfacesAndSelfTo<PlacesRenderer>().FromComponentOnRoot().AsSingle();

        Container.BindInstance(Camera.main.GetComponent<PanAndZoom>());

        Container.Bind<InputField>().WithId("SearchBox").FromInstance(searchBox.GetComponent<InputField>());

        Container.Bind<TextAsset>().WithId("Secrets").FromResources("Secrets").AsSingle();

        Container.Bind<Canvas>().FromComponentOn(worldCanvas).AsSingle();

        Container.BindInterfacesAndSelfTo<ViewportService>().AsSingle().NonLazy();
        Container.BindInterfacesAndSelfTo<PlacesService>().AsSingle().NonLazy();
        Container.BindInterfacesAndSelfTo<SecretsService>().AsSingle().NonLazy();
        Container.BindInterfacesAndSelfTo<SearchService>().AsSingle().NonLazy();
    }
}
