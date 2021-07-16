config.output.publicPath = "resources/js/"

config.output.library = "main"
config.output.filename = (chunkData) => {
    return chunkData.chunk.name === "main"
        ? "main.js"
        : "main-[name].js";
}
