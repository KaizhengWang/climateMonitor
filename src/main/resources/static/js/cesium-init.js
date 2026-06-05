Cesium.Ion.defaultAccessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJjMzVjOTMwNS1lZGQyLTQxYTgtYmQ2Mi1lZjVlMjkxOWM3M2MiLCJpZCI6Mzg0MjU1LCJpYXQiOjE3Njk1ODAxNDN9.rsVCe43I-OGnuJhnCaeVk2B6t0k0PsnwSXFHPwr67w8";

// 创建 Viewer
const viewer = new Cesium.Viewer('cesiumContainer', {
    animation: false,
    timeline: false,
    baseLayerPicker: false,
    navigationHelpButton: false,
    geocoder: false,
    homeButton: false,
    sceneModePicker: false,
    infoBox: false,
    selectionIndicator: false,
    fullscreenButton: true
});

// 澳门全景初始视角
viewer.camera.setView({
    destination: Cesium.Cartesian3.fromDegrees(113.5530, 22.1240, 30000.0),
    orientation: {
        heading: Cesium.Math.toRadians(0),
        pitch: Cesium.Math.toRadians(-83),
        roll: 0.0
    }
});

// 调整滚轮缩放灵敏度
const controller = viewer.scene.screenSpaceCameraController;
controller.zoomEventMultiplier = 0.05; // 缩放更慢
controller.inertiaSpin = 0.0;         // 关闭旋转惯性
controller.inertiaTranslate = 0.0;    // 关闭平移惯性
controller.inertiaZoom = 0.5;         // 轻度缩放惯性

// ---------- 加载澳门边界 GeoJSON ----------
Cesium.GeoJsonDataSource.load('/geojson/macau_boundaryv2.geojson', {
    stroke: Cesium.Color.RED,               // 边框红色
    fill: Cesium.Color.fromAlpha(Cesium.Color.WHITE, 0.1), // 半透明白填充
    strokeWidth: 2,
    clampToGround: true
}).then(dataSource => {
    viewer.dataSources.add(dataSource);
});

// ---------- 加载澳门建筑3D Tiles模型 ----------
Cesium.Cesium3DTileset.fromUrl('/macao_building/building/3dtiles/tileset.json')
    .then(tileset => {
        viewer.scene.primitives.add(tileset);
        viewer.zoomTo(tileset);
    })
    .catch(e => console.warn('澳门建筑模型加载失败:', e.message));

// 创建站点实体
const stationEntities = {};
stations.forEach(station => {
    const isSelf = station.source === 'mqtt';

    const entity = {
        id: station.id,
        name: station.displayName || station.name,
        position: Cesium.Cartesian3.fromDegrees(station.lon, station.lat, 50),
        // 透明的点击判定区域
        ellipse: {
            semiMinorAxis: 25,
            semiMajorAxis: 25,
            material: Cesium.Color.TRANSPARENT,
            outline: false
        },
        label: {
            text: station.displayName || station.name,
            font: '20px "Microsoft YaHei", bold',
            fillColor: Cesium.Color.BLACK,
            outlineColor: Cesium.Color.WHITE,
            outlineWidth: 3,
            style: Cesium.LabelStyle.FILL_AND_OUTLINE,
            verticalOrigin: Cesium.VerticalOrigin.BOTTOM,
            pixelOffset: new Cesium.Cartesian2(0, -25),
            disableDepthTestDistance: Number.POSITIVE_INFINITY
        }
    };

    if (isSelf) {
        // 自研站：蓝色三角
        entity.billboard = {
            image: 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22"><polygon points="11,1 21,21 1,21" fill="#3182CE" stroke="#1A5A9A" stroke-width="1"/></svg>'),
            width: 22,
            height: 22,
            disableDepthTestDistance: Number.POSITIVE_INFINITY,
            verticalOrigin: Cesium.VerticalOrigin.BOTTOM
        };
    } else {
        // 外部站：红色圆点
        entity.point = {
            pixelSize: 14,
            color: Cesium.Color.fromCssColorString('#E74C3C'),
            disableDepthTestDistance: Number.POSITIVE_INFINITY
        };
    }

    stationEntities[station.id] = viewer.entities.add(entity);
});

// 点击事件：只处理 stations 中的实体
const handler = new Cesium.ScreenSpaceEventHandler(viewer.scene.canvas);
handler.setInputAction(click => {
    const picked = viewer.scene.pick(click.position);

    if (picked && picked.id) {
        const clickedStation = stations.find(s => s.id === picked.id.id);
        if (clickedStation) {
            updateInfoPanel(clickedStation.id); // 更新面板
            return;
        }
    }

    // 点击到其他地方或非站点实体，一律关闭面板
    closeInfoPanel();

}, Cesium.ScreenSpaceEventType.LEFT_CLICK);
