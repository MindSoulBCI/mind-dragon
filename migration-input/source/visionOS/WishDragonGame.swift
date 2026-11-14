import SwiftUI
import RealityKit
import Combine
import AVFoundation

@MainActor final class WishDragonSession: ObservableObject {
    static let shared = WishDragonSession()
    let root = Entity()
    // Manipulation owns the outer transform; animation only changes these inner layers.
    private let dragGroup = Entity()
    private let liftGroup = Entity()
    private let spinGroup = Entity()
    private let dragon = Entity()
    private var halo: ModelEntity?
    private let dragonMotion=Entity()
    private var ballMotions:[Entity]=[]
    private var ballHolders:[Entity]=[]
    private var ballHomes:[SIMD3<Float>]=[]
    private var ballAuras:[Entity]=[]
    private var movingParts:[(Entity,simd_quatf)]=[]
    private var loading=false
    static let recallRequested = Notification.Name("WishDragonRecallRequested")
    private var dragonSkins:[Entity]=[]
    private var starParts:[Entity]=[]
    private var brandLogos:[Entity]=[]
    @Published var skin=UserDefaults.standard.integer(forKey:"wish-dragon-skin") {
        didSet {UserDefaults.standard.set(skin,forKey:"wish-dragon-skin");applySkin(); recall()}
    }
    @Published private(set) var progress = WishFocusProgress()
    @Published private(set) var loaded = false
    @Published private(set) var error: String?
    @Published private(set) var focus: Float?
    private var charge: AVAudioPlayer?
    private var summon: AVAudioPlayer?
    private var orbitTime: Float = 0
    private var started = false
    func load() async {
        guard !loaded, !loading else { return }
        error=nil
        loading=true;defer {loading=false}
        do {
            let balls = try await Entity(named: "wish-balls.usdz")
            let original = try await Entity(named: "wish-dragon-motion.usdz")
            let branded = try await Entity(named: "wish-dragon-mindsoul.usdz")
            dragonSkins=[branded,original]
            let aura = try await Entity(named: "wish-gold-aura.usdz")
            root.children.removeAll(); spinGroup.children.removeAll()
            applyGlass(to: balls)
            splitBalls(balls,aura:aura)
            guard ballHolders.count == 7 else { throw CocoaError(.fileReadCorruptFile) }
            liftGroup.addChild(spinGroup);dragGroup.addChild(liftGroup);root.addChild(dragGroup)
            dragon.children.removeAll();dragon.addChild(dragonMotion);root.addChild(dragon)
            applySkin()
            for (file, isCharge) in [("wish-charge", true), ("wish-summon", false)] {
                if let url = Bundle.main.url(forResource: file, withExtension: "mp3") {
                    guard let player = try? AVAudioPlayer(contentsOf: url) else { continue }; player.prepareToPlay()
                    if isCharge { player.numberOfLoops = -1; charge = player } else { summon = player }
                }
            }
            loaded = true; reset()
        } catch { self.error = "模型加载失败：\(error.localizedDescription)" }
    }
    /// Each ball owns its gesture transform; the parent owns gathering and floating.
    private func splitBalls(_ source:Entity,aura:Entity) {
        func meshes(_ node:Entity)->[Entity] {(node.components[ModelComponent.self] == nil ? []:[node])+node.children.flatMap{meshes($0)}}
        let parts=meshes(source)
        let centers=parts.filter{$0.name.contains("02")}.map{$0.visualBounds(relativeTo:source).center}
        ballMotions=[];ballHolders=[];ballHomes=[];ballAuras=[];starParts=[];brandLogos=[]
        var logoMaterial:UnlitMaterial?
        if let cg=UIImage(named:"OpeningLogo")?.cgImage,let texture=try? TextureResource.generate(from:cg,options:.init(semantic:.color)) {
            var material=UnlitMaterial();material.color = .init(tint:.white,texture:.init(texture))
            material.blending = .transparent(opacity:.init(floatLiteral:1));material.faceCulling = .none;logoMaterial=material
        }
        for (i,center) in centers.enumerated() {
            let motion=Entity();let holder=Entity();holder.name="wish-ball-\(i+1)"
            for part in parts {
                let c=part.visualBounds(relativeTo:source).center
                let nearest=centers.indices.min{simd_distance_squared(centers[$0],c)<simd_distance_squared(centers[$1],c)}
                guard nearest == i else {continue}
                let copy=part.clone(recursive:true);copy.transform=Transform(matrix:part.transformMatrix(relativeTo:source));copy.position-=center;holder.addChild(copy)
                if !part.name.contains("02") {starParts.append(copy)}
            }
            let b=holder.visualBounds(relativeTo:holder);let size=max(b.extents.x,max(b.extents.y,b.extents.z))
            let visual=Entity();for child in Array(holder.children) {visual.addChild(child)}
            visual.scale = .init(repeating:0.22/max(size,0.001));holder.addChild(visual)
            makeDraggable(holder)
            if let material=logoMaterial {
                let logo=ModelEntity(mesh:.generatePlane(width:0.14,height:0.14),materials:[material])
                logo.name="MindSoul-Ball-Logo";logo.components.set(BillboardComponent())
                holder.addChild(logo);brandLogos.append(logo)
            }
            let glow=aura.clone(recursive:true);glow.isEnabled=false;holder.addChild(glow);ballAuras.append(glow)
            let angle=Float(i)*2 * .pi/Float(max(1,centers.count))
            let home=SIMD3<Float>(sin(angle)*0.72,0,cos(angle)*0.28)
            motion.position=home;motion.addChild(holder);spinGroup.addChild(motion)
            ballMotions.append(motion);ballHolders.append(holder);ballHomes.append(home)
        }
    }
    private func applySkin() {
        guard dragonSkins.count == 2 else{return}
        for (part,rest) in movingParts {part.orientation=rest}
        let index=skin == 1 ? 1:0
        dragonMotion.children.removeAll()
        let model=dragonSkins[index]
        let bounds = model.visualBounds(relativeTo:model)
        let scale = 1.24 / max(0.001,bounds.extents.y)
        model.scale = .init(repeating:scale)
        model.position = [0,0.30,-0.20] - bounds.center * scale
        dragonMotion.addChild(model);movingParts=[]
        func capture(_ node:Entity) {
            if node.name.hasPrefix("WishWhisker") || node.name.hasPrefix("WishJaw") {movingParts.append((node,node.orientation))}
            for child in node.children {capture(child)}
        }
        capture(model);makeDraggable(dragon)
        for star in starParts {star.isEnabled=index == 1}
        for logo in brandLogos {logo.isEnabled=index == 0}
    }
    private func applyGlass(to entity: Entity) {
        if var model = entity.components[ModelComponent.self] {
            model.materials = model.materials.map { material in
                guard var glass = material as? PhysicallyBasedMaterial else { return material }
                if entity.name.contains("02") {
                    glass.baseColor.tint = .init(red: 1, green: 0.63, blue: 0.16, alpha: 1)
                    glass.blending = .transparent(opacity: .init(floatLiteral: 0.42))
                    glass.roughness = .init(floatLiteral: 0.08)
                    glass.metallic = .init(floatLiteral: 0.35)
                    glass.emissiveColor = .init(color: .init(red: 1, green: 0.62, blue: 0.02, alpha: 1))
                    glass.emissiveIntensity = 1.15
                } else {
                    glass.emissiveColor = .init(color: .red)
                    glass.emissiveIntensity = 1.4
                }
                return glass
            }
            entity.components.set(model)
        }
        for child in entity.children { applyGlass(to: child) }
    }
    private func makeDraggable(_ entity: Entity) {
        // Hit the visible surfaces, not one large box covering the empty orbit space.
        // All child surfaces redirect both hands to the same manipulation owner.
        entity.components.remove(CollisionComponent.self)
        entity.components.remove(InputTargetComponent.self)
        entity.components.remove(HoverEffectComponent.self)
        var style = HoverEffectComponent.HighlightHoverEffectStyle.default
        style.strength = 1.5
        style.opacityFunction = .full
        let hover = HoverEffectComponent(.highlight(style, groupID: .init()))
        func configureSurfaces(_ node: Entity) {
            guard node.name != "wish-charge-glow" else { return }
            if node.components[ModelComponent.self] != nil {
                node.generateCollisionShapes(recursive: false)
                node.components.set(InputTargetComponent(allowedInputTypes: .all))
                node.components.set(hover)
                node.components.set(ManipulationComponent.HitTarget(redirectedEntity: entity))
            }
            for child in node.children { configureSurfaces(child) }
        }
        configureSurfaces(entity)
        var gesture = ManipulationComponent()
        var dynamics = ManipulationComponent.Dynamics()
        dynamics.translationBehavior = .unconstrained
        dynamics.primaryRotationBehavior = .unconstrained
        dynamics.secondaryRotationBehavior = .unconstrained
        dynamics.scalingBehavior = .unconstrained
        dynamics.inertia = .low
        gesture.dynamics = dynamics; gesture.releaseBehavior = .stay
        entity.components.set(gesture)
    }
    func recall() {
        NotificationCenter.default.post(name:Self.recallRequested,object:nil)
        dragGroup.transform = .identity
        dragon.transform = .identity
        for holder in ballHolders { holder.transform = .identity }
    }
    func begin() {
        guard loaded, GameConnectionState.shared.canOperate else { return }
        reset(); started = true; progress.start()
        try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
    }
    func reset() {
        stop(); progress.reset(); orbitTime = 0; recall()
        dragGroup.transform = .identity; liftGroup.transform = .identity; spinGroup.transform = .identity
        liftGroup.position = [0, -0.30, 0.08]
        dragon.transform = .identity;dragonMotion.transform = .identity;dragon.isEnabled = false;halo?.isEnabled = false
        for i in ballMotions.indices {ballMotions[i].position=ballHomes[i];ballHolders[i].transform = .identity;ballAuras[i].isEnabled=false}
        for (part,orientation) in movingParts {part.orientation=orientation}
    }
    func stop() { started = false; charge?.stop(); summon?.stop(); progress.reset() }
    func tick(_ dt: Float) {
        guard loaded else { return }
        focus = GameFlightInput.shared.effectiveFocus
        guard started else { return }
        let old = progress.phase
        progress.advance(dt, focus: focus, allowed: GameConnectionState.shared.canOperate, simulation: GameFlightInput.shared.simulateFocus)
        let charging = progress.phase == .focusing && progress.held > 0
        if charging { charge?.volume = 0.15 + 0.3 * (focus ?? 0); if charge?.isPlaying == false { charge?.play() } }
        else { charge?.stop() }
        if progress.phase == .flashing {
            // A slow warm pulse, rather than a high-frequency flashing stimulus.
            halo?.isEnabled = true
            let amount=min(1,progress.flashTime/1.5)
            for i in ballMotions.indices {
                let cluster=ballHomes[i]*0.43
                ballMotions[i].position=simd_mix(ballHomes[i],cluster,SIMD3<Float>(repeating:amount))
                ballAuras[i].isEnabled=true
                ballAuras[i].scale = .init(repeating:1+0.12*sin(progress.flashTime*2))
            }
            let pulse = 1 + 0.06 * sin(progress.flashTime * .pi * 2)
            spinGroup.scale = SIMD3<Float>(repeating: pulse)
        }
        if progress.phase == .summoned {
            if old != .summoned { dragon.isEnabled = true; summon?.currentTime = 0; summon?.play(); halo?.isEnabled = false; spinGroup.scale = .one }
            orbitTime += dt
            liftGroup.position.y = -0.30 + min(0.15, orbitTime * 0.10)
            spinGroup.orientation = simd_quatf(angle: orbitTime * 0.16, axis: [0, 1, 0])
            for i in ballMotions.indices {
                let cluster=ballHomes[i]*0.43
                ballMotions[i].position=cluster+SIMD3<Float>(0,sin(orbitTime*0.7+Float(i))*0.045,0)
                ballAuras[i].isEnabled=true
                ballAuras[i].scale = .init(repeating:1+0.06*sin(orbitTime*1.3))
            }
            dragonMotion.position=[sin(orbitTime*0.18)*0.08,sin(orbitTime*0.5)*0.07,0]
            dragonMotion.orientation=simd_quatf(angle:sin(orbitTime*0.22)*0.045,axis:[0,1,0])
            for (index,pair) in movingParts.enumerated() {
                let (part,rest)=pair
                let jaw=part.name.hasPrefix("WishJaw")
                let angle=jaw ? (1+sin(orbitTime*1.1))*0.055 : sin(orbitTime*0.8+Float(index))*0.10
                part.orientation=rest*simd_quatf(angle:angle,axis:jaw ? [1,0,0]:[0,0,1])
            }
            // Gesture transforms stay on the holders, so animation never resets a dragged ball.

        }
    }
}

struct WishDragonGameView: View {
    @ObservedObject private var game = WishDragonSession.shared
    @ObservedObject private var input = GameFlightInput.shared
    @ObservedObject private var connection = GameConnectionState.shared
    @EnvironmentObject private var coordinator: ImmersiveTransitionCoordinator
    @Environment(\.openWindow) private var openWindow
    @Environment(\.dismissWindow) private var dismissWindow
    private var status: String {
        switch game.progress.phase {
        case .idle: "准备许愿 · 保持专注力 ≥70% 超过 10 秒"
        case .focusing: game.progress.held > 0 ? "请保持专注，正在汇聚龙珠能量" : "专注不足或数据中断，连续计时已清零"
        case .flashing: "龙珠响应 · 正在召唤神龙"
        case .summoned: "许愿成功 · 龙珠升空环绕，可继续抓取拖动"
        }
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack { VStack(alignment: .leading) { Text("许愿神龙").font(.title2.bold()); Text("专注力体验 · 非商用").font(.caption).foregroundStyle(.secondary) }; Spacer(); Button("返回游戏列表") { GamePresentationLifecycle.returnToList(openWindow: openWindow, dismissWindow: dismissWindow) } }
            GameConnectionControl()
            HStack {
                Text("选择版本").font(.headline)
                Button("MindSoul 版") {game.skin=0}.tint(game.skin == 0 ? .blue:.gray)
                Button("神龙版本（仅限测试非商用）") {game.skin=1}.tint(game.skin == 1 ? .green:.gray)
            }
            Text(game.skin == 0 ? "蓝色神龙 · 橙金透明圆球 · MindSoul Logo" : "原版绿色神龙 · 星星龙珠 · 仅限测试非商用").font(.caption).foregroundStyle(.secondary)
            HStack { Text("专注力").font(.headline); Spacer(); Text(game.focus.map { "\(Int($0 * 100))%" } ?? "—").font(.title.bold()).monospacedDigit() }
            ProgressView(value: Double(game.focus ?? 0)).tint(.mint)
            Text(input.sourceLabel).font(.caption).foregroundStyle(.secondary)
            if input.simulateFocus { Slider(value: $input.simulatedFocus, in: 0...1) { Text("模拟专注力") } }
            HStack { Text("连续达标"); Spacer(); Text(String(format: "%.1f / 10.0 秒", min(game.progress.held, 10))).monospacedDigit() }
            ProgressView(value: Double(min(game.progress.held / 10, 1))).tint(.orange)
            Text(game.error ?? status).font(.headline)
            HStack { Button(game.progress.phase == .idle ? "开始许愿" : "重新许愿") { game.begin() }.disabled(!game.loaded || !connection.canOperate); Button("召回龙珠与神龙") { game.recall() } }
            Text("注视龙珠 / 神龙高亮 · 捏合拖动\n双手同时捏合：拉开放大、合拢缩小\n试玩不生成真实 EEG 记录；本游戏仅为非商用体验。").font(.caption).foregroundStyle(.secondary)
            Text("模型：Anthony Yanez · CC BY 4.0（已转换尺寸与格式）\n音效：Kenney · CC0").font(.system(size: 10)).foregroundStyle(.secondary)
        }.padding(24).frame(width: 680).glassBackgroundEffect()
        .onAppear {
            GamePresentationLifecycle.active.insert("wish-dragon"); GamePresentationLifecycle.coordinator = coordinator
            GameSpatialState.shared.kind = 8; coordinator.requestSwitch(to: .gameSpatial)
            GameConnectionState.shared.start(); input.start(); dismissWindow(id: WindowID.mainMenu)
            GameAuxiliaryWindows.arrange(game: 8, openWindow: openWindow, dismissWindow: dismissWindow)
        }
        .task {
            #if DEBUG
            if ProcessInfo.processInfo.arguments.contains("--preview-dragon-summon") {
                for _ in 0..<100 {
                    if game.loaded { break }
                    try? await Task.sleep(for: .milliseconds(100))
                    if Task.isCancelled { return }
                }
                input.simulateFocus = true; input.simulatedFocus = 0.8; game.begin()
            }
            #endif
        }
        .onDisappear { GamePresentationLifecycle.active.remove("wish-dragon"); game.stop() }
    }
}
