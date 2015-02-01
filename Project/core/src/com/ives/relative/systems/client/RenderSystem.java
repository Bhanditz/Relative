package com.ives.relative.systems.client;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Wire;
import com.artemis.managers.TagManager;
import com.artemis.systems.EntityProcessingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.ives.relative.entities.components.body.Position;
import com.ives.relative.entities.components.client.Visual;
import com.ives.relative.universe.chunks.Chunk;
import com.ives.relative.universe.chunks.ChunkManager;

/**
 * Created by Ives on 3/12/2014.
 * This system renders every entity
 */
@Wire
public class RenderSystem extends EntityProcessingSystem {
    protected ComponentMapper<Position> mPosition;
    protected ComponentMapper<Visual> visualMapper;
    protected ChunkManager chunkManager;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;

    public RenderSystem(SpriteBatch batch, OrthographicCamera camera) {
        //noinspection unchecked
        super(Aspect.getAspectForAll(Visual.class, Position.class));
        this.batch = batch;
        this.camera = camera;
        this.shapeRenderer = new ShapeRenderer();
    }

    @Override
    protected void begin() {
        super.begin();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        positionCamera();
        renderBackground();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

    }

    @Override
    protected void process(Entity entity) {
        Position position = mPosition.get(entity);
        Visual visual = visualMapper.get(entity);
        batch.draw(visual.texture,
                position.x - visual.width / 2, position.y - visual.height / 2,
                visual.width / 2, visual.height / 2,
                visual.width, visual.height,
                1.02f, 1.02f,
                position.rotation * MathUtils.radiansToDegrees);
    }

    @Override
    protected void end() {
        super.end();
        batch.end();
    }

    private void positionCamera() {
        Entity player = world.getManager(TagManager.class).getEntity("player");

        //If player has spawned
        if (player != null) {
            Position playerPosition = mPosition.get(player);
            camera.position.x = playerPosition.x;
            camera.position.y = playerPosition.y + 4;

            //float rotation = playerPosition.rotation * MathUtils.radiansToDegrees;
            //float camrotation = -getCurrentCameraRotation() + 180;
            //camera.rotate(camrotation - rotation + 180);
            camera.update();
            batch.setProjectionMatrix(camera.combined);
        }
    }

    private void renderBackground() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Chunk chunk : chunkManager.getLoadedChunks()) {
            if (chunk.backgroundColor != Color.BLACK) {
                Color color = chunk.backgroundColor;
                shapeRenderer.rect(chunk.x, chunk.y, chunk.universeBody.chunkSize, chunk.universeBody.chunkSize, color, color, color, color);
            }
        }
        shapeRenderer.end();
    }

    private float getCurrentCameraRotation() {
        return (float) Math.atan2(camera.up.x, camera.up.y) * MathUtils.radiansToDegrees;
    }
}
