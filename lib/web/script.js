(() => {
  "use strict";

  /* ============================================================
     DogApi — espelha a classe Api\DogApi do projeto PHP
     ============================================================ */
  class DogApi {
    static BASE = "https://dog.ceo/api";

    /**
     * Retorna todas as raças achatadas em uma lista de itens.
     * Equivalente ao allBreeds() do PHP, mas já transformado para
     * a estrutura que a UI consome.
     *
     * @returns {Promise<Array<{breed:string, sub:string|null, label:string}>>}
     */
    async allBreeds() {
      const res = await fetch(`${DogApi.BASE}/breeds/list/all`);
      if (!res.ok) throw new Error("Falha ao carregar a lista de raças.");
      const json = await res.json();

      const out = [];
      for (const [breed, subs] of Object.entries(json.message)) {
        if (subs.length) {
          for (const sub of subs) {
            out.push({ breed, sub, label: `${sub} ${breed}` });
          }
        } else {
          out.push({ breed, sub: null, label: breed });
        }
      }
      out.sort((a, b) => a.label.localeCompare(b.label, "en"));
      return out;
    }

    /**
     * Retorna a URL de uma imagem aleatória da raça/sub-raça.
     * Equivalente ao getImage($breed) do PHP.
     */
    async getImage(breed, sub = null) {
      const path = sub
        ? `${DogApi.BASE}/breed/${breed}/${sub}/images/random`
        : `${DogApi.BASE}/breed/${breed}/images/random`;

      const res = await fetch(path);
      if (!res.ok) throw new Error("Falha ao buscar imagem.");
      const json = await res.json();
      return json.message;
    }
  }

  /* ============================================================
     BreedIndex — estado e render da lista lateral
     ============================================================ */
  class BreedIndex {
    constructor(root, input, onSelect) {
      this.root = root;
      this.input = input;
      this.onSelect = onSelect;

      this.breeds = [];
      this.view = [];
      this.activeSlug = null;

      this.input.addEventListener("input", () => this.render());
    }

    static slug(item) {
      return item.sub ? `${item.breed}-${item.sub}` : item.breed;
    }

    static pad(n, w) {
      return String(n).padStart(w, "0");
    }

    setBreeds(breeds) {
      this.breeds = breeds;
      this.render();
    }

    get activeItem() {
      return this.breeds.find((b) => BreedIndex.slug(b) === this.activeSlug) || null;
    }

    setActive(item) {
      this.activeSlug = BreedIndex.slug(item);

      for (const node of this.root.querySelectorAll(".item")) {
        node.classList.toggle("is-active", node.dataset.slug === this.activeSlug);
      }

      const current = this.root.querySelector(`.item[data-slug="${this.activeSlug}"]`);
      if (current) current.scrollIntoView({ block: "nearest", inline: "nearest" });
    }

    render() {
      const term = this.input.value.trim().toLowerCase();
      this.view = term
        ? this.breeds.filter((b) => b.label.includes(term))
        : this.breeds;

      const frag = document.createDocumentFragment();

      for (const item of this.view) {
        const index = this.breeds.indexOf(item);
        const id = BreedIndex.slug(item);

        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "item";
        btn.dataset.slug = id;
        btn.innerHTML =
          `<span class="n">${BreedIndex.pad(index + 1, 3)}</span>` +
          `<span class="lbl">${item.label}</span>`;

        if (this.activeSlug === id) btn.classList.add("is-active");
        btn.addEventListener("click", () => this.onSelect(item));

        frag.appendChild(btn);
      }

      this.root.replaceChildren(frag);
    }
  }

  /* ============================================================
     DogViewer — estado e render do painel principal
     ============================================================ */
  class DogViewer {
    constructor(refs, api) {
      this.refs = refs;    // { title, idx, total, frame, photo, empty, taxo, shuffle }
      this.api = api;

      this.token = 0;
      this.current = null;

      this.refs.photo.addEventListener("load", () => {
        this.refs.photo.classList.add("is-ready");
      });

      this.refs.shuffle.addEventListener("click", () => this.refresh());
    }

    setTotal(n) {
      this.refs.total.textContent = BreedIndex.pad(n, 3);
    }

    async show(item, index, total) {
      this.current = item;

      this.refs.idx.textContent = BreedIndex.pad(index, 2);
      this.refs.title.textContent = item.label;
      this.refs.taxo.textContent = item.sub ? `${item.breed} / ${item.sub}` : item.breed;
      this.refs.shuffle.disabled = false;

      this.refs.title.animate(
        [{ opacity: 0, transform: "translateY(8px)" }, { opacity: 1, transform: "none" }],
        { duration: 380, easing: "cubic-bezier(.2,.7,.2,1)" }
      );

      await this.loadImage(item);
    }

    async refresh() {
      if (this.current) await this.loadImage(this.current);
    }

    async loadImage(item) {
      const token = ++this.token;
      this.refs.frame.classList.add("is-loading");

      try {
        const url = await this.api.getImage(item.breed, item.sub);
        if (token !== this.token) return;

        const pre = new Image();
        pre.src = url;
        if (pre.decode) await pre.decode().catch(() => {});
        if (token !== this.token) return;

        this.refs.photo.classList.remove("is-ready");
        this.refs.photo.src = url;
        this.refs.photo.alt = item.label;
        this.refs.empty.hidden = true;
      } catch (_) {
        // mantém a imagem anterior em caso de falha
      } finally {
        if (token === this.token) this.refs.frame.classList.remove("is-loading");
      }
    }
  }

  /* ============================================================
     App — orquestra API, lista e painel
     ============================================================ */
  class App {
    constructor() {
      const refs = {
        title:   document.getElementById("title"),
        idx:     document.getElementById("idx"),
        total:   document.getElementById("total"),
        frame:   document.getElementById("frame"),
        photo:   document.getElementById("photo"),
        empty:   document.getElementById("empty"),
        taxo:    document.getElementById("taxo"),
        shuffle: document.getElementById("shuffle"),
      };

      this.api = new DogApi();
      this.viewer = new DogViewer(refs, this.api);
      this.index = new BreedIndex(
        document.getElementById("list"),
        document.getElementById("q"),
        (item) => this.select(item)
      );

      document.addEventListener("keydown", (e) => this.onKey(e));
      window.addEventListener("hashchange", () => this.fromHash());
    }

    async init() {
      try {
        const breeds = await this.api.allBreeds();
        this.index.setBreeds(breeds);
        this.viewer.setTotal(breeds.length);

        const initial = this.findByHash() || breeds[0];
        if (initial) this.select(initial, false);
      } catch (err) {
        this.viewer.refs.empty.textContent = "Não foi possível carregar o arquivo.";
      }
    }

    select(item, pushHash = true) {
      const index = this.index.breeds.indexOf(item) + 1;
      this.index.setActive(item);
      this.viewer.show(item, index, this.index.breeds.length);

      if (pushHash) history.replaceState(null, "", `#${BreedIndex.slug(item)}`);
    }

    findByHash() {
      const slug = decodeURIComponent(location.hash.slice(1));
      if (!slug) return null;
      return this.index.breeds.find((b) => BreedIndex.slug(b) === slug) || null;
    }

    fromHash() {
      const target = this.findByHash();
      if (target && target !== this.index.activeItem) this.select(target, false);
    }

    onKey(e) {
      if (e.target.tagName === "INPUT") return;
      const view = this.index.view;
      if (!view.length) return;

      const current = this.index.activeItem;
      const i = view.indexOf(current);

      if (e.key === "ArrowDown" || e.key === "j") {
        e.preventDefault();
        const next = view[Math.min(i + 1, view.length - 1)];
        if (next && next !== current) this.select(next);
      }

      if (e.key === "ArrowUp" || e.key === "k") {
        e.preventDefault();
        const prev = view[Math.max(i - 1, 0)];
        if (prev && prev !== current) this.select(prev);
      }
    }
  }

  /* ---------- bootstrap ---------- */

  const app = new App();
  app.init();
})();